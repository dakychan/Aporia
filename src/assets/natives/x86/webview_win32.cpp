#include <windows.h>
#include <wrl.h>
#include <WebView2.h>
#include <wincodec.h>
#include <string>
#include <vector>
#include <stdlib.h>

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
#define PW_RENDERFULLCONTENT 0x00000002
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
    fprintf(stderr, "[Aporia] webview_create: enter %dx%d url=%S\n", width, height, url ? url : L"null");

    HRESULT hr = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
    if (FAILED(hr)) {
        fprintf(stderr, "[Aporia] webview_create: CoInitializeEx failed hr=0x%08lx\n", hr);
        return NULL;
    }

    WebviewInstance* inst = new WebviewInstance();
    inst->width = width;
    inst->height = height;
    inst->ready = false;
    inst->failed = false;
    inst->capturing = false;
    inst->createDoneEvent = CreateEventW(NULL, FALSE, FALSE, NULL);

    HANDLE pumpThread = CreateThread(NULL, 0, MessagePumpThread, NULL, 0, NULL);
    CloseHandle(pumpThread);

    HMODULE hModDll = NULL;
    GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS,
        (LPCWSTR)&webview_create, &hModDll);
    if (!hModDll) hModDll = GetModuleHandleW(L"webview_native.dll");
    if (!hModDll) hModDll = GetModuleHandleW(NULL);

    const wchar_t CLASS_NAME[] = L"WebviewHiddenWindow";
    WNDCLASSW wc = {};
    wc.lpfnWndProc = WndProc;
    wc.hInstance = hModDll;
    wc.lpszClassName = CLASS_NAME;
    wc.hbrBackground = (HBRUSH)GetStockObject(BLACK_BRUSH);
    RegisterClassW(&wc);

    // HWND must be VISIBLE for DWM composition — PrintWindow needs it.
    // HWND_BOTTOM + no topmost = behind Minecraft fullscreen = invisible to user.
    inst->hwnd = CreateWindowExW(WS_EX_TOOLWINDOW | WS_EX_NOACTIVATE, CLASS_NAME, L"W",
        WS_POPUP | WS_VISIBLE,
        0, 0, width, height, NULL, NULL, hModDll, NULL);
    SetWindowPos(inst->hwnd, HWND_BOTTOM, 0, 0, 0, 0,
        SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);

    if (!inst->hwnd) {
        fprintf(stderr, "[Aporia] webview_create: CreateWindowExW failed err=%lu\n", GetLastError());
        CloseHandle(inst->createDoneEvent);
        delete inst;
        return NULL;
    }

    wchar_t dllFn[MAX_PATH];
    GetModuleFileNameW(hModDll, dllFn, MAX_PATH);
    wchar_t* lastSlash = wcsrchr(dllFn, L'\\');
    if (lastSlash) lastSlash[1] = L'\0';

    std::wstring baseDir = dllFn;
    std::wstring runtimePath = baseDir + L"Microsoft.WebView2.FixedVersionRuntime.148.0.3967.70.x64";

    std::wstring userDataPath;
    std::wstring passedPath = utf8_to_wstring(userDataFolderUtf8);
    if (!passedPath.empty()) {
        userDataPath = passedPath;
    } else {
        userDataPath = baseDir + L"webview_userdata";
    }

    DWORD attr = GetFileAttributesW((runtimePath + L"\\msedgewebview2.exe").c_str());
    bool useSystemRuntime = (attr == INVALID_FILE_ATTRIBUTES);

    const wchar_t* rtPtr = useSystemRuntime ? NULL : runtimePath.c_str();
    hr = CreateCoreWebView2EnvironmentWithOptions(
        rtPtr, userDataPath.c_str(), NULL,
        Callback<ICoreWebView2CreateCoreWebView2EnvironmentCompletedHandler>(
            [inst](HRESULT result, ICoreWebView2Environment* env) -> HRESULT {
                if (FAILED(result)) {
                    inst->failed = true;
                    SetEvent(inst->createDoneEvent);
                    return result;
                }
                inst->env = env;
                env->CreateCoreWebView2Controller(inst->hwnd,
                    Callback<ICoreWebView2CreateCoreWebView2ControllerCompletedHandler>(
                        [inst](HRESULT result, ICoreWebView2Controller* c) -> HRESULT {
                            if (FAILED(result)) {
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
                            fprintf(stderr, "[Aporia] webview_create: Controller OK\n");
                            return S_OK;
                        }).Get());
                return S_OK;
            }).Get());

    if (FAILED(hr)) {
        CloseHandle(inst->createDoneEvent);
        DestroyWindow(inst->hwnd);
        delete inst;
        return NULL;
    }

    return inst;
}

__declspec(dllexport) int webview_wait_ready(void* handle, int timeoutMs) {
    if (!handle) return 0;
    WebviewInstance* inst = (WebviewInstance*)handle;
    if (inst->ready) return 1;
    if (inst->failed) return 0;

    DWORD signaledIndex = 0;
    HRESULT hrCo = CoWaitForMultipleHandles(
        COWAIT_DISPATCH_WINDOW_MESSAGES,
        (DWORD)timeoutMs, 1, &inst->createDoneEvent, &signaledIndex);

    if (FAILED(hrCo) && !inst->ready && !inst->failed) {
        DWORD start = GetTickCount();
        while (!inst->ready && !inst->failed) {
            if (GetTickCount() - start >= (DWORD)timeoutMs) break;
            MSG msg;
            while (PeekMessage(&msg, NULL, 0, 0, PM_REMOVE)) {
                TranslateMessage(&msg);
                DispatchMessage(&msg);
            }
            if (WaitForSingleObject(inst->createDoneEvent, 10) == WAIT_OBJECT_0) break;
        }
    }

    MSG msg;
    while (PeekMessage(&msg, NULL, 0, 0, PM_REMOVE)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
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
    return ((WebviewInstance*)handle)->failed ? 1 : 0;
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
    SetWindowPos(inst->hwnd, HWND_BOTTOM, screenX, screenY, pixelW, pixelH, SWP_NOACTIVATE);
    if (inst->controller) {
        RECT r = {0, 0, pixelW, pixelH};
        inst->controller->put_Bounds(r);
    }
}

__declspec(dllexport) void webview_hide(void* handle) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    // Keep HWND at bottom — visible to DWM but behind everything
    SetWindowPos(inst->hwnd, HWND_BOTTOM, 0, 0, 0, 0,
        SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
    fprintf(stderr, "[Aporia] webview_hide: HWND_BOTTOM (DWM-visible)\n");
}

__declspec(dllexport) void webview_set_focus(void* handle) {
    if (!handle) return;
    auto* inst = (WebviewInstance*)handle;
    if (inst->controller) {
        inst->controller->MoveFocus(COREWEBVIEW2_MOVE_FOCUS_REASON_PROGRAMMATIC);
    }
    SetFocus(inst->hwnd);
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
    if (!handle) return 0;
    auto* inst = (WebviewInstance*)handle;
    if (!inst->ready || !inst->webview) return 0;
    if (inst->capturing) return 0;
    inst->capturing = true;

    int w = inst->width;
    int h = inst->height;
    if (w <= 0 || h <= 0) { inst->capturing = false; return 0; }
    int needed = w * h * 4;
    if (!outBuf || outBufSize < needed) {
        inst->capturing = false;
        return needed;
    }

    // PrintWindow + CreateDIBSection: raw BGRA, zero encoding/decoding
    HDC hdcScreen = GetDC(NULL);
    HDC hdcMem = CreateCompatibleDC(hdcScreen);

    BITMAPINFO bmi = {};
    bmi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bmi.bmiHeader.biWidth = w;
    bmi.bmiHeader.biHeight = -h;
    bmi.bmiHeader.biPlanes = 1;
    bmi.bmiHeader.biBitCount = 32;
    bmi.bmiHeader.biCompression = BI_RGB;

    void* bits = NULL;
    HBITMAP hBitmap = CreateDIBSection(hdcMem, &bmi, DIB_RGB_COLORS, &bits, NULL, 0);
    HBITMAP hOldBmp = (HBITMAP)SelectObject(hdcMem, hBitmap);

    // Force the window to paint — needed after move offscreen
    InvalidateRect(inst->hwnd, NULL, FALSE);
    UpdateWindow(inst->hwnd);

    BOOL ok = PrintWindow(inst->hwnd, hdcMem, PW_RENDERFULLCONTENT);

    // Fallback: BitBlt if PrintWindow failed
    if (!ok || !bits) {
        HDC hdcWnd = GetDC(inst->hwnd);
        if (hdcWnd) {
            BitBlt(hdcMem, 0, 0, w, h, hdcWnd, 0, 0, SRCCOPY);
            ok = TRUE;
            ReleaseDC(inst->hwnd, hdcWnd);
        }
    }

    if (ok && bits) {
        memcpy(outBuf, bits, (size_t)needed);
    } else {
        memset(outBuf, 0, (size_t)needed);
    }

    SelectObject(hdcMem, hOldBmp);
    DeleteObject(hBitmap);
    DeleteDC(hdcMem);
    ReleaseDC(NULL, hdcScreen);

    inst->capturing = false;
    return needed;
}

__declspec(dllexport) int webview_get_dpi(void* handle) {
    if (!handle) return 96;
    auto* inst = (WebviewInstance*)handle;
    if (!inst->hwnd) return 96;
    return GetDpiForWindow(inst->hwnd);
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
