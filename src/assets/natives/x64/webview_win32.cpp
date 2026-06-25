#include <windows.h>
#include <wrl.h>
#include <WebView2.h>
#include <wincodec.h>
#include <string>
#include <vector>

#ifndef GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS
#define GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS 0x00000004
#endif

#ifndef COWAIT_DISPATCH_WINDOW_MESSAGES
#define COWAIT_DISPATCH_WINDOW_MESSAGES 0x10
#endif
#ifndef COWAIT_DISPATCH_CALLS
#define COWAIT_DISPATCH_CALLS 0x20
#endif
#ifndef PW_RENDERFULLCONTENT
#define PW_RENDERFULLCONTENT 0x00000003
#endif

using namespace Microsoft::WRL;

struct WebviewInstance {
    HWND hwnd;
    ComPtr<ICoreWebView2Environment> env;
    ComPtr<ICoreWebView2Controller> controller;
    ComPtr<ICoreWebView2> webview;
    int width, height;
    bool ready;
    bool failed;
    bool capturing;
    HANDLE createDoneEvent;
};

static LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM w, LPARAM l) {
    return DefWindowProc(hwnd, msg, w, l);
}

static DWORD WINAPI MessagePumpThread(LPVOID param) {
    MSG msg;
    while (GetMessage(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
    return 0;
}

class VectorStream : public IStream {
    std::vector<unsigned char>& data;
    LONG refCount;
    size_t pos;
public:
    VectorStream(std::vector<unsigned char>& d) : data(d), refCount(1), pos(0) {}

    HRESULT STDMETHODCALLTYPE QueryInterface(REFIID riid, void** ppv) override {
        if (riid == IID_IUnknown || riid == IID_IStream || riid == IID_ISequentialStream) {
            *ppv = this;
            AddRef();
            return S_OK;
        }
        *ppv = NULL;
        return E_NOINTERFACE;
    }

    ULONG STDMETHODCALLTYPE AddRef() override {
        return InterlockedIncrement(&refCount);
    }

    ULONG STDMETHODCALLTYPE Release() override {
        ULONG r = InterlockedDecrement(&refCount);
        if (r == 0) delete this;
        return r;
    }

    HRESULT STDMETHODCALLTYPE Read(void* pv, ULONG cb, ULONG* pcbRead) override {
        ULONG available = (ULONG)(data.size() - pos);
        ULONG toRead = min(cb, available);
        memcpy(pv, data.data() + pos, toRead);
        pos += toRead;
        if (pcbRead) *pcbRead = toRead;
        return (toRead == cb) ? S_OK : S_FALSE;
    }

    HRESULT STDMETHODCALLTYPE Write(const void* pv, ULONG cb, ULONG* pcbWritten) override {
        size_t newPos = pos + cb;
        if (newPos > data.size()) data.resize(newPos);
        memcpy(data.data() + pos, pv, cb);
        pos = newPos;
        if (pcbWritten) *pcbWritten = cb;
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE Seek(LARGE_INTEGER dlibMove, DWORD dwOrigin, ULARGE_INTEGER* plibNewPosition) override {
        switch (dwOrigin) {
            case STREAM_SEEK_SET: pos = (size_t)dlibMove.QuadPart; break;
            case STREAM_SEEK_CUR: pos += (size_t)dlibMove.QuadPart; break;
            case STREAM_SEEK_END: pos = data.size() + (size_t)dlibMove.QuadPart; break;
        }
        if (plibNewPosition) plibNewPosition->QuadPart = pos;
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE SetSize(ULARGE_INTEGER) override { return E_NOTIMPL; }
    HRESULT STDMETHODCALLTYPE CopyTo(IStream*, ULARGE_INTEGER, ULARGE_INTEGER*, ULARGE_INTEGER*) override { return E_NOTIMPL; }
    HRESULT STDMETHODCALLTYPE Commit(DWORD) override { return S_OK; }
    HRESULT STDMETHODCALLTYPE Revert() override { return E_NOTIMPL; }
    HRESULT STDMETHODCALLTYPE LockRegion(ULARGE_INTEGER, ULARGE_INTEGER, DWORD) override { return E_NOTIMPL; }
    HRESULT STDMETHODCALLTYPE UnlockRegion(ULARGE_INTEGER, ULARGE_INTEGER, DWORD) override { return E_NOTIMPL; }
    HRESULT STDMETHODCALLTYPE Stat(STATSTG* pstatstg, DWORD) override {
        pstatstg->cbSize.QuadPart = data.size();
        pstatstg->type = STGTY_STREAM;
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE Clone(IStream**) override { return E_NOTIMPL; }
};

// Convert UTF-8 char* to wstring (outside extern "C" because it returns C++ type)
static std::wstring utf8_to_wstring(const char* utf8) {
    if (!utf8 || !utf8[0]) return L"";
    int len = MultiByteToWideChar(CP_UTF8, 0, utf8, -1, NULL, 0);
    if (len <= 0) return L"";
    std::wstring ws(len - 1, L'\0');
    MultiByteToWideChar(CP_UTF8, 0, utf8, -1, &ws[0], len);
    return ws;
}

extern "C" {

__declspec(dllexport) void* webview_create(int width, int height, const wchar_t* url, const char* userDataFolderUtf8) {
    fprintf(stderr, "[Aporia] webview_create: enter %dx%d url=%S userData=%s\n", width, height, url ? url : L"null", userDataFolderUtf8 ? userDataFolderUtf8 : "null");

    HRESULT hr = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
    if (FAILED(hr)) {
        fprintf(stderr, "[Aporia] webview_create: CoInitializeEx failed hr=0x%08lx\n", hr);
        return NULL;
    }
    fprintf(stderr, "[Aporia] webview_create: CoInitializeEx OK\n");

    WebviewInstance* inst = new WebviewInstance();
    inst->width = width;
    inst->height = height;
    inst->ready = false;
    inst->failed = false;
    inst->capturing = false;
    inst->createDoneEvent = CreateEventW(NULL, FALSE, FALSE, NULL);

    HANDLE pumpThread = CreateThread(NULL, 0, MessagePumpThread, NULL, 0, NULL);
    CloseHandle(pumpThread);

    // Get this DLL's own path
    HMODULE hModDll = NULL;
    GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS,
        (LPCWSTR)&webview_create, &hModDll);
    fprintf(stderr, "[Aporia] webview_create: GetModuleHandleExW hModDll=%p\n", (void*)hModDll);
    if (!hModDll) {
        hModDll = GetModuleHandleW(L"webview_native.dll");
        fprintf(stderr, "[Aporia] webview_create: fallback GetModuleHandleW(dll) hModDll=%p\n", (void*)hModDll);
    }
    if (!hModDll) {
        hModDll = GetModuleHandleW(NULL);
        fprintf(stderr, "[Aporia] webview_create: fallback GetModuleHandleW(NULL) hModDll=%p\n", (void*)hModDll);
    }

    const wchar_t CLASS_NAME[] = L"WebviewHiddenWindow";
    WNDCLASSW wc = {};
    wc.lpfnWndProc = WndProc;
    wc.hInstance = hModDll;
    wc.lpszClassName = CLASS_NAME;
    RegisterClassW(&wc);

    inst->hwnd = CreateWindowExW(0, CLASS_NAME, L"W", WS_POPUP,
        0, 0, width, height, NULL, NULL, hModDll, NULL);

    if (!inst->hwnd) {
        DWORD err = GetLastError();
        fprintf(stderr, "[Aporia] webview_create: CreateWindowExW failed err=%lu\n", err);
        CloseHandle(inst->createDoneEvent);
        delete inst;
        return NULL;
    }
    fprintf(stderr, "[Aporia] webview_create: CreateWindowExW OK hwnd=%p\n", (void*)inst->hwnd);

    wchar_t dllFn[MAX_PATH];
    GetModuleFileNameW(hModDll, dllFn, MAX_PATH);
    wchar_t* lastSlash = wcsrchr(dllFn, L'\\');
    if (lastSlash) lastSlash[1] = L'\0';

    std::wstring baseDir = dllFn;
    std::wstring runtimePath = baseDir + L"Microsoft.WebView2.FixedVersionRuntime.148.0.3967.70.x64";

    // User data folder: use passed path (UTF-8), fall back to DLL-relative
    std::wstring userDataPath;
    std::wstring passedPath = utf8_to_wstring(userDataFolderUtf8);
    if (!passedPath.empty()) {
        userDataPath = passedPath;
    } else {
        userDataPath = baseDir + L"webview_userdata";
    }
    fprintf(stderr, "[Aporia] webview_create: using userDataPath=%S\n", userDataPath.c_str());

    DWORD attr = GetFileAttributesW((runtimePath + L"\\msedgewebview2.exe").c_str());
    bool useSystemRuntime = (attr == INVALID_FILE_ATTRIBUTES);
    fprintf(stderr, "[Aporia] webview_create: baseDir=%S useSystemRuntime=%d userDataPath=%S\n",
        baseDir.c_str(), (int)useSystemRuntime, userDataPath.c_str());

    const wchar_t* rtPtr = useSystemRuntime ? NULL : runtimePath.c_str();
    hr = CreateCoreWebView2EnvironmentWithOptions(
        rtPtr, userDataPath.c_str(), NULL,
        Callback<ICoreWebView2CreateCoreWebView2EnvironmentCompletedHandler>(
            [inst](HRESULT result, ICoreWebView2Environment* env) -> HRESULT {
                if (FAILED(result)) {
                    fprintf(stderr, "[Aporia] webview_create: Environment callback failed hr=0x%08lx\n", result);
                    inst->failed = true;
                    SetEvent(inst->createDoneEvent);
                    return result;
                }
                inst->env = env;
                env->CreateCoreWebView2Controller(inst->hwnd,
                    Callback<ICoreWebView2CreateCoreWebView2ControllerCompletedHandler>(
                        [inst](HRESULT result, ICoreWebView2Controller* c) -> HRESULT {
                            if (FAILED(result)) {
                                fprintf(stderr, "[Aporia] webview_create: Controller callback failed hr=0x%08lx\n", result);
                                inst->failed = true;
                                SetEvent(inst->createDoneEvent);
                                return result;
                            }
                            inst->controller = c;
                            c->get_CoreWebView2(&inst->webview);
                            RECT r = {0, 0, inst->width, inst->height};
                            c->put_Bounds(r);
                            c->put_IsVisible(TRUE);
                            ComPtr<ICoreWebView2Settings> settings;
                            if (SUCCEEDED(inst->webview->get_Settings(&settings))) {
                                settings->put_AreDefaultScriptDialogsEnabled(FALSE);
                                settings->put_IsScriptEnabled(TRUE);
                            }
                            inst->ready = true;
                            SetEvent(inst->createDoneEvent);
                            fprintf(stderr, "[Aporia] webview_create: Controller callback OK\n");
                            return S_OK;
                        }).Get());
                return S_OK;
            }).Get());

    if (FAILED(hr)) {
        fprintf(stderr, "[Aporia] webview_create: CreateCoreWebView2Environment failed synchronously hr=0x%08lx\n", hr);
        CloseHandle(inst->createDoneEvent);
        DestroyWindow(inst->hwnd);
        delete inst;
        return NULL;
    }

    fprintf(stderr, "[Aporia] webview_create: returning inst=%p\n", (void*)inst);
    return inst;
}

__declspec(dllexport) int webview_wait_ready(void* handle, int timeoutMs) {
    if (!handle) return 0;
    WebviewInstance* inst = (WebviewInstance*)handle;
    fprintf(stderr, "[Aporia] webview_wait_ready: enter ready=%d failed=%d timeout=%d\n",
        (int)inst->ready, (int)inst->failed, timeoutMs);
    if (inst->ready) { fprintf(stderr, "[Aporia] webview_wait_ready: already ready\n"); return 1; }
    if (inst->failed) { fprintf(stderr, "[Aporia] webview_wait_ready: already failed\n"); return 0; }

    // Try CoWaitForMultipleHandles (proper COM-aware wait)
    DWORD signaledIndex = 0;
    HRESULT hrCo = CoWaitForMultipleHandles(
        COWAIT_DISPATCH_WINDOW_MESSAGES,
        (DWORD)timeoutMs,
        1,
        &inst->createDoneEvent,
        &signaledIndex);

    // If CoWait failed (e.g. not in STA), fall back to manual pumping
    if (FAILED(hrCo) && !inst->ready && !inst->failed) {
        fprintf(stderr, "[Aporia] webview_wait_ready: CoWait failed hr=0x%08lx, falling back to PeekMessage loop\n", hrCo);
        DWORD start = GetTickCount();
        while (!inst->ready && !inst->failed) {
            if (GetTickCount() - start >= (DWORD)timeoutMs) break;
            MSG msg;
            while (PeekMessage(&msg, NULL, 0, 0, PM_REMOVE)) {
                TranslateMessage(&msg);
                DispatchMessage(&msg);
            }
            // Also wait on the event with a short timeout to avoid tight loop
            if (WaitForSingleObject(inst->createDoneEvent, 10) == WAIT_OBJECT_0) break;
        }
    }

    // Final message pump
    MSG msg;
    while (PeekMessage(&msg, NULL, 0, 0, PM_REMOVE)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }

    fprintf(stderr, "[Aporia] webview_wait_ready: done ready=%d failed=%d\n",
        (int)inst->ready, (int)inst->failed);
    return inst->ready ? 1 : 0;
}

__declspec(dllexport) int webview_is_ready(void* handle) {
    if (!handle) return 0;
    WebviewInstance* inst = (WebviewInstance*)handle;
    MSG msg;
    while (PeekMessage(&msg, NULL, 0, 0, PM_REMOVE)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
    return inst->ready ? 1 : 0;
}

__declspec(dllexport) int webview_is_failed(void* handle) {
    if (!handle) return 1;
    WebviewInstance* inst = (WebviewInstance*)handle;
    MSG msg;
    while (PeekMessage(&msg, NULL, 0, 0, PM_REMOVE)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
    return inst->failed ? 1 : 0;
}

__declspec(dllexport) void webview_destroy(void* handle) {
    if (!handle) return;
    WebviewInstance* inst = (WebviewInstance*)handle;
    if (inst->webview) inst->webview->Stop();
    if (inst->controller) inst->controller->Close();
    if (inst->hwnd) DestroyWindow(inst->hwnd);
    CloseHandle(inst->createDoneEvent);
    delete inst;
}

__declspec(dllexport) void webview_navigate(void* handle, const wchar_t* url) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    if (inst->webview) inst->webview->Navigate(url);
}

__declspec(dllexport) void webview_load_html(void* handle, const wchar_t* html) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    if (inst->webview) inst->webview->NavigateToString(html);
}

__declspec(dllexport) void webview_show(void* handle, int screenX, int screenY, int pixelW, int pixelH) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    inst->width = pixelW;
    inst->height = pixelH;
    SetWindowPos(inst->hwnd, HWND_NOTOPMOST, screenX, screenY, pixelW, pixelH, SWP_SHOWWINDOW);
    if (inst->controller) {
        RECT r = {0, 0, pixelW, pixelH};
        inst->controller->put_Bounds(r);
    }
    BringWindowToTop(inst->hwnd);
    fprintf(stderr, "[Aporia] webview_show: %dx%d @ %d,%d\n", pixelW, pixelH, screenX, screenY);
}

__declspec(dllexport) void webview_hide(void* handle) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    ShowWindow(inst->hwnd, SW_HIDE);
    fprintf(stderr, "[Aporia] webview_hide\n");
}

__declspec(dllexport) void webview_set_focus(void* handle) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    if (inst->controller) {
        inst->controller->MoveFocus(COREWEBVIEW2_MOVE_FOCUS_REASON_PROGRAMMATIC);
    }
    SetFocus(inst->hwnd);
    fprintf(stderr, "[Aporia] webview_set_focus\n");
}

__declspec(dllexport) void webview_resize(void* handle, int width, int height) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    inst->width = width;
    inst->height = height;
    if (inst->controller) {
        RECT r = {0, 0, width, height};
        inst->controller->put_Bounds(r);
    }
}

__declspec(dllexport) int webview_capture_rgba(void* handle, unsigned char* outBuf, int outBufSize) {
    if (!handle) { fprintf(stderr, "[A] cap: null handle\n"); return 0; }
    auto* inst = (WebviewInstance*)handle;
    if (!inst->ready || !inst->webview) { fprintf(stderr, "[A] cap: not ready\n"); return 0; }

    // Prevent overlapping captures (WebView2 rejects concurrent CapturePreview)
    if (inst->capturing) { fprintf(stderr, "[A] cap: skip — already capturing\n"); return 0; }
    inst->capturing = true;

    int w = inst->width;
    int h = inst->height;
    if (w <= 0 || h <= 0) { inst->capturing = false; return 0; }
    int needed = w * h * 4;
    if (!outBuf || outBufSize < needed) {
        inst->capturing = false;
        return needed;
    }

    fprintf(stderr, "[A] cap: start %dx%d\n", w, h);

    // Step 1: Capture WebView2 preview as JPEG (faster than PNG)
    // Use PNG (JPEG causes crashes with some WebView2 runtimes)
    std::vector<unsigned char> imgData;
    HANDLE done = CreateEventW(NULL, FALSE, FALSE, NULL);
    auto* stream = new VectorStream(imgData);

    inst->webview->CapturePreview(
        COREWEBVIEW2_CAPTURE_PREVIEW_IMAGE_FORMAT_PNG,
        stream,
        Callback<ICoreWebView2CapturePreviewCompletedHandler>(
            [done](HRESULT hr) -> HRESULT {
                SetEvent(done);
                return S_OK;
            }).Get());

    DWORD start = GetTickCount();
    while (true) {
        MSG msg;
        while (PeekMessage(&msg, NULL, 0, 0, PM_REMOVE)) {
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
        if (WAIT_OBJECT_0 == WaitForSingleObject(done, 10)) break;
        if (GetTickCount() - start > 1000) break;
    }
    CloseHandle(done);

    if (imgData.empty()) {
        fprintf(stderr, "[A] cap: no PNG data\n");
        stream->Release();
        inst->capturing = false;
        return 0;
    }

    // Step 2: Decode PNG → BGRA via WIC
    IStream* wicStream = NULL;
    if (FAILED(CreateStreamOnHGlobal(NULL, TRUE, &wicStream)) || !wicStream) {
        stream->Release();
        inst->capturing = false;
        return 0;
    }
    wicStream->Write(imgData.data(), (ULONG)imgData.size(), NULL);
    wicStream->Seek({0}, STREAM_SEEK_SET, NULL);

    IWICImagingFactory* wicFactory = NULL;
    CoCreateInstance(CLSID_WICImagingFactory, NULL, CLSCTX_INPROC_SERVER,
        IID_IWICImagingFactory, (void**)&wicFactory);

    if (wicFactory) {
        IWICBitmapDecoder* decoder = NULL;
        if (SUCCEEDED(wicFactory->CreateDecoderFromStream(wicStream, NULL,
                WICDecodeMetadataCacheOnLoad, &decoder)) && decoder) {

            IWICBitmapFrameDecode* frame = NULL;
            if (SUCCEEDED(decoder->GetFrame(0, &frame)) && frame) {
                UINT fw, fh;
                frame->GetSize(&fw, &fh);

                UINT cw = min(fw, (UINT)w);
                UINT ch = min(fh, (UINT)h);
                UINT bufSz = cw * ch * 4;

                IWICFormatConverter* converter = NULL;
                if (SUCCEEDED(wicFactory->CreateFormatConverter(&converter)) && converter) {
                    converter->Initialize(frame, GUID_WICPixelFormat32bppBGRA,
                        WICBitmapDitherTypeNone, NULL, 0.0, WICBitmapPaletteTypeCustom);
                    converter->CopyPixels(NULL, cw * 4, bufSz, outBuf);
                    converter->Release();
                }

                if (cw < (UINT)w || ch < (UINT)h) {
                    for (int y = ch; y < h; y++)
                        memset(outBuf + y * w * 4, 0, (size_t)w * 4);
                    for (int y = 0; y < (int)ch; y++)
                        memset(outBuf + y * w * 4 + cw * 4, 0, (size_t)(w - cw) * 4);
                }
                frame->Release();
            }
            decoder->Release();
        }
        wicFactory->Release();
    }
    wicStream->Release();
    stream->Release();

    inst->capturing = false;
    fprintf(stderr, "[A] cap: done %d bytes (BGRA)\n", needed);
    return needed;
}

__declspec(dllexport) void webview_send_mouse_move(void* handle, int x, int y) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    SendMessageW(inst->hwnd, WM_MOUSEMOVE, 0, MAKELPARAM(x, y));
}

__declspec(dllexport) void webview_send_mouse_press(void* handle, int x, int y, int button) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    UINT msg = (button == 0) ? WM_LBUTTONDOWN : (button == 1) ? WM_RBUTTONDOWN : WM_MBUTTONDOWN;
    WPARAM w = (button == 0) ? MK_LBUTTON : (button == 1) ? MK_RBUTTON : MK_MBUTTON;
    SendMessageW(inst->hwnd, msg, w, MAKELPARAM(x, y));
}

__declspec(dllexport) void webview_send_mouse_release(void* handle, int x, int y, int button) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    UINT msg = (button == 0) ? WM_LBUTTONUP : (button == 1) ? WM_RBUTTONUP : WM_MBUTTONUP;
    SendMessageW(inst->hwnd, msg, 0, MAKELPARAM(x, y));
}

__declspec(dllexport) void webview_send_mouse_wheel(void* handle, double delta) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    SendMessageW(inst->hwnd, WM_MOUSEWHEEL, MAKEWPARAM(0, (short)(delta * WHEEL_DELTA)), 0);
}

__declspec(dllexport) void webview_send_key_press(void* handle, int vk, int scancode, int modifiers) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    SendMessageW(inst->hwnd, WM_KEYDOWN, vk, MAKELPARAM(scancode, 1));
}

__declspec(dllexport) void webview_send_key_release(void* handle, int vk, int scancode, int modifiers) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    SendMessageW(inst->hwnd, WM_KEYUP, vk, MAKELPARAM(scancode, 1 | (1 << 30)));
}

__declspec(dllexport) void webview_send_key_char(void* handle, wchar_t c) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    SendMessageW(inst->hwnd, WM_CHAR, c, 1);
}

} // extern "C"