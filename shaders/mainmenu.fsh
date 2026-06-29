#version 150

uniform float u_time;
uniform vec2 u_resolution;

in vec2 v_texCoord;
out vec4 fragColor;

// Дешевый хэш
float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// Дешевый шум
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

// Вращение UV
vec2 rotate(vec2 uv, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec2(c * uv.x - s * uv.y, s * uv.x + c * uv.y);
}

// Дешевая замена division-based glow
float cheapGlow(float d, float radius) {
    return exp(-d * d / (radius * radius));
}

void main() {
    vec2 uv = v_texCoord;
    float t = u_time;
    
    // Медленное вращение неба
    vec2 rotUV = rotate(uv - 0.5, t * 0.03) + 0.5;
    
    vec3 col = vec3(0.02, 0.01, 0.05);
    
    // 1. Фоновый градиент
    float dist = length(uv - 0.5);
    col += vec3(0.02, 0.005, 0.04) * (1.0 - dist * 1.5);
    
    // 2. Космическая пыль (один сэмпл шума)
    float n = noise(rotUV * 4.0 + t * 0.1); 
    col += mix(vec3(0.0), vec3(0.15, 0.05, 0.3), n) * smoothstep(0.5, 0.8, n) * 0.4;
    
    // 3. Вспышки (2 вместо 4)
    for (int i = 0; i < 2; i++) {
        float fi = float(i);
        float cycle = 12.0 + hash(vec2(fi, 3.7)) * 10.0;
        float phase = mod(t + hash(vec2(fi, 1.3)) * cycle, cycle);
        float life = 1.0 - phase / cycle;
        float intensity = smoothstep(0.0, 0.1, life) * smoothstep(1.0, 0.2, life);
        intensity *= intensity * (1.0 + 0.3 * sin(t * 3.0 + fi * 5.0));
        
        vec2 pos = vec2(hash(vec2(fi, 12.98)), hash(vec2(fi, 78.23)));
        float d = length(rotUV - pos);
        col += cheapGlow(d, 0.08) * intensity * vec3(0.5, 0.15, 0.9) * 0.4;
    }
    
    // 4. Звёзды через ГРИД (4 ячейки вместо 9)
    float gridSize = 6.0;
    vec2 gridUV = rotUV * gridSize;
    vec2 gridId = floor(gridUV);
    vec2 gridF = fract(gridUV) - 0.5;
    
    for (int y = 0; y <= 1; y++) {
        for (int x = 0; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec2 cellId = gridId + neighbor;
            vec2 starPos = vec2(hash(cellId), hash(cellId + 99.0)) - 0.5;
            vec2 diff = gridF - neighbor - starPos;
            
            float drift = t * 0.15;
            diff += vec2(sin(drift + hash(cellId) * 6.28) * 0.15,
                         cos(drift + hash(cellId + 10.0) * 6.28) * 0.15);
            
            float d = length(diff);
            float twinkle = sin(t * (1.5 + hash(cellId + 50.0) * 2.5) + hash(cellId + 20.0) * 6.28) * 0.5 + 0.5;
            twinkle = twinkle * twinkle;
            
            vec3 starCol = mix(vec3(0.7, 0.6, 1.0), vec3(1.0, 0.9, 0.8), hash(cellId + 70.0));
            col += cheapGlow(d, 0.04) * twinkle * starCol * 0.35;
        }
    }
    
    // 5. Метеоры (1 вместо 2)
    float speed = 0.35;
    float cycle = 2.0 / speed;
    float phase = mod(t * speed, cycle);
    float life = phase / cycle;
    
    vec2 startPos = vec2(0.8, 0.9);
    vec2 vel = normalize(vec2(-0.8, -0.3));
    vec2 pos = startPos + vel * phase;
    vec2 diff = uv - pos;
    
    float along = dot(diff, vel);
    float across = length(diff - vel * along);
    float fade = smoothstep(0.05, 0.0, -along) * smoothstep(0.6, 0.0, along);
    col += fade * cheapGlow(across, 0.01) * vec3(0.8, 0.6, 1.0) * (1.0 - life) * 0.5;
    
    // 6. Зернистость (дешевле)
    col += (hash(uv * 500.0 + fract(t)) - 0.5) * 0.03;
    
    // 7. Виньетка
    col *= smoothstep(0.9, 0.3, dist * 1.5);
    
    col = clamp(col, 0.0, 1.0);
    col = pow(col, vec3(0.95));
    
    fragColor = vec4(col, 1.0);
}
