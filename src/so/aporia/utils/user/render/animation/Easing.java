/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.animation;

/**
 * Easing functions — all take {@code t} in [0,1] and return a value in ~[0,1].
 * <p>
 * Categories:
 * <ul>
 *   <li><b>Polynomial</b> — Quad, Cubic, Quart, Quint</li>
 *   <li><b>Trigonometric</b> — Sine, Circ</li>
 *   <li><b>Exponential</b> — Expo</li>
 *   <li><b>Back</b> — overshoots past 1 then settles</li>
 *   <li><b>Elastic</b> — spring oscillation</li>
 *   <li><b>Bounce</b> — bouncing ball</li>
 *   <li><b>Physics</b> — spring, gravity, pendulum, critically-damped</li>
 *   <li><b>Special</b> — smoothstep, smootherstep, bezier, catmull-rom</li>
 * </ul>
 */
public final class Easing {

    private static final float PI  = (float) Math.PI;
    private static final float TAU = PI * 2f;

    private Easing() {}

    private static float clamp(float t) { return Math.max(0f, Math.min(1f, t)); }

    public static float linear(float t) { return clamp(t); }

    public static float quadIn   (float t) { t=clamp(t); return t*t; }
    public static float quadOut  (float t) { t=clamp(t); return 1-(1-t)*(1-t); }
    public static float quadInOut(float t) { t=clamp(t); return t<.5f ? 2*t*t : 1-2*(1-t)*(1-t); }

    public static float cubicIn   (float t) { t=clamp(t); return t*t*t; }
    public static float cubicOut  (float t) { t=clamp(t); float u=1-t; return 1-u*u*u; }
    public static float cubicInOut(float t) { t=clamp(t); return t<.5f ? 4*t*t*t : 1-4*(1-t)*(1-t)*(1-t); }

    public static float quartIn   (float t) { t=clamp(t); return t*t*t*t; }
    public static float quartOut  (float t) { t=clamp(t); float u=1-t; return 1-u*u*u*u; }
    public static float quartInOut(float t) { t=clamp(t); return t<.5f ? 8*t*t*t*t : 1-8*(1-t)*(1-t)*(1-t)*(1-t); }

    public static float quintIn   (float t) { t=clamp(t); return t*t*t*t*t; }
    public static float quintOut  (float t) { t=clamp(t); float u=1-t; return 1-u*u*u*u*u; }
    public static float quintInOut(float t) { t=clamp(t); return t<.5f ? 16*t*t*t*t*t : 1-16*(1-t)*(1-t)*(1-t)*(1-t)*(1-t); }

    public static float sineIn   (float t) { t=clamp(t); return 1-(float)Math.cos(t*PI/2); }
    public static float sineOut  (float t) { t=clamp(t); return (float)Math.sin(t*PI/2); }
    public static float sineInOut(float t) { t=clamp(t); return .5f-.5f*(float)Math.cos(t*PI); }

    public static float expoIn   (float t) { t=clamp(t); return t==0?0:(float)Math.pow(2,10*t-10); }
    public static float expoOut  (float t) { t=clamp(t); return t==1?1:1-(float)Math.pow(2,-10*t); }
    public static float expoInOut(float t) {
        t=clamp(t);
        if(t==0) return 0; if(t==1) return 1;
        return t<.5f ? (float)Math.pow(2,20*t-10)/2 : (2-(float)Math.pow(2,-20*t+10))/2;
    }

    public static float circIn   (float t) { t=clamp(t); return 1-(float)Math.sqrt(1-t*t); }
    public static float circOut  (float t) { t=clamp(t); return (float)Math.sqrt(1-(t-1)*(t-1)); }
    public static float circInOut(float t) {
        t=clamp(t);
        return t<.5f ? (1-(float)Math.sqrt(1-4*t*t))/2 : ((float)Math.sqrt(1-(2*t-2)*(2*t-2))+1)/2;
    }

    private static final float C1 = 1.70158f;
    private static final float C2 = C1 * 1.525f;
    private static final float C3 = C1 + 1f;

    public static float backIn   (float t) { t=clamp(t); return C3*t*t*t - C1*t*t; }
    public static float backOut  (float t) { t=clamp(t); float u=t-1; return 1+C3*u*u*u+C1*u*u; }
    public static float backInOut(float t) {
        t=clamp(t);
        return t<.5f ? (4*t*t*((C2+1)*2*t-C2))/2
                     : (4*(t-1)*(t-1)*((C2+1)*(2*t-2)+C2)+2)/2;
    }

    /**
     * Back with custom overshoot amount (default C1 = 1.70158).
     * <p>
     * Back с пользовательским значением overshoot.
     */
    public static float backOut(float t, float overshoot) {
        t=clamp(t); float u=t-1; float c=overshoot+1;
        return 1 + c*u*u*u + overshoot*u*u;
    }

    private static final float E1 = TAU / 3f;
    private static final float E2 = TAU / 4.5f;

    public static float elasticIn(float t) {
        t=clamp(t);
        if(t==0) return 0; if(t==1) return 1;
        return -(float)(Math.pow(2,10*t-10)*Math.sin((t*10-10.75)*E1));
    }
    public static float elasticOut(float t) {
        t=clamp(t);
        if(t==0) return 0; if(t==1) return 1;
        return (float)(Math.pow(2,-10*t)*Math.sin((t*10-0.75)*E1))+1;
    }
    public static float elasticInOut(float t) {
        t=clamp(t);
        if(t==0) return 0; if(t==1) return 1;
        return t<.5f
            ? -(float)(Math.pow(2,20*t-10)*Math.sin((20*t-11.125)*E2))/2
            :  (float)(Math.pow(2,-20*t+10)*Math.sin((20*t-11.125)*E2))/2+1;
    }

    /**
     * Elastic with custom amplitude and period.
     * <p>
     * Elastic с пользовательской амплитудой и периодом.
     */
    public static float elasticOut(float t, float amplitude, float period) {
        t=clamp(t);
        if(t==0) return 0; if(t==1) return 1;
        float s = (float)(period/(TAU)*Math.asin(1/amplitude));
        return (float)(amplitude*Math.pow(2,-10*t)*Math.sin((t-s)*TAU/period))+1;
    }

    public static float bounceOut(float t) {
        t=clamp(t);
        float n1=7.5625f, d1=2.75f;
        if(t<1/d1)           return n1*t*t;
        else if(t<2/d1)      { t-=1.5f/d1;   return n1*t*t+0.75f; }
        else if(t<2.5f/d1)   { t-=2.25f/d1;  return n1*t*t+0.9375f; }
        else                 { t-=2.625f/d1; return n1*t*t+0.984375f; }
    }
    public static float bounceIn   (float t) { return 1-bounceOut(1-t); }
    public static float bounceInOut(float t) {
        return t<.5f ? (1-bounceOut(1-2*t))/2 : (1+bounceOut(2*t-1))/2;
    }

    /**
     * Ken Perlin's smoothstep — C1 continuous.
     * <p>
     * Ken Perlin smoothstep — C1 непрерывный.
     */
    public static float smoothstep(float t) { t=clamp(t); return t*t*(3-2*t); }

    /**
     * Smootherstep — C2 continuous (zero 1st and 2nd derivatives at edges).
     * <p>
     * Smootherstep — C2 непрерывный (нулевые 1-я и 2-я производные на краях).
     */
    public static float smootherstep(float t) { t=clamp(t); return t*t*t*(t*(t*6-15)+10); }

    /**
     * Smootheststep — C3 continuous.
     * <p>
     * Smootheststep — C3 непрерывный.
     */
    public static float smootheststep(float t) { t=clamp(t); return t*t*t*t*(t*(t*(t*(-20)+70)-84)+35); }

    /**
     * Critically-damped spring response.
     * {@code t} is normalized time [0,1], {@code stiffness} controls speed (try 6–12).
     * Returns position in [0,1] that approaches 1 asymptotically.
     * <p>
     * Критически затухающая пружина.
     * {@code t} — нормализованное время [0,1], {@code stiffness} контролирует скорость (6–12).
     */
    public static float springCritical(float t, float stiffness) {
        t=clamp(t);
        float x = stiffness * t;
        return 1f - (1f + x) * (float)Math.exp(-x);
    }

    /**
     * Under-damped spring — oscillates around 1 before settling.
     * {@code damping} in (0,1): lower = more oscillation.
     * {@code frequency} controls oscillation speed (try 2–6).
     */
    public static float springUnderdamped(float t, float damping, float frequency) {
        t=clamp(t);
        if(t==0) return 0;
        float w  = frequency * TAU;
        float wd = w * (float)Math.sqrt(1 - damping*damping);
        return 1f - (float)(Math.exp(-damping*w*t) * Math.cos(wd*t));
    }

    /**
     * Gravity drop — accelerates like free-fall, then bounces.
     * {@code bounces} = number of bounces (0 = no bounce, just fall).
     * <p>
     * Гравитационное падение — ускоряется как свободное падение, затем отскакивает.
     * {@code bounces} — количество отскоков (0 = без отскока).
     */
    public static float gravity(float t, int bounces) {
        t=clamp(t);
        if(bounces<=0) return t*t;
        float result = 0;
        float h = 1f;
        float tLeft = t;
        for(int i=0; i<=bounces; i++) {
            float segLen = (float)Math.sqrt(h) * 0.4f;
            if(tLeft <= segLen) {
                float u = tLeft / segLen;
                result = h * (1 - (2*u-1)*(2*u-1));
                return result;
            }
            tLeft -= segLen;
            h *= 0.5f;
        }
        return 1f;
    }

    /**
     * Pendulum swing — sinusoidal with natural deceleration.
     * Returns 0→1 with a slight overshoot arc.
     */
    public static float pendulum(float t) {
        t=clamp(t);
        return (float)(1 - Math.cos(t * PI * 0.5) * Math.exp(-t * 2.5));
    }

    /**
     * Rubber-band — fast start, elastic overshoot, snap back to 1.
     */
    public static float rubberBand(float t) {
        t=clamp(t);
        return 1f + (float)(Math.pow(2,-10*t) * Math.sin((t-0.1)*TAU/0.4));
    }

    /**
     * Anticipation — pulls back slightly before launching forward (like a slingshot).
     * {@code pullback} controls how far back it goes (try 0.1–0.3).
     */
    public static float anticipate(float t, float pullback) {
        t=clamp(t);
        if(t < 0.2f) {
            float u = t / 0.2f;
            return -pullback * u * u;
        }
        float u = (t - 0.2f) / 0.8f;
        return backOut(u, 1.5f) * (1 + pullback) - pullback;
    }

    /**
     * Overshoot-and-settle — goes past 1, comes back.
     * {@code overshoot} = how far past 1 (try 0.2–0.5).
     */
    public static float overshoot(float t, float overshoot) {
        t=clamp(t);
        return 1f + overshoot * (float)(Math.sin(t * PI) * Math.exp(-t * 4));
    }

    /**
     * Cubic bezier easing — equivalent to CSS {@code cubic-bezier(x1,y1,x2,y2)}.
     * Solves for t via Newton's method then evaluates Y.
     * <p>
     * Кубический bezier — эквивалент CSS {@code cubic-bezier(x1,y1,x2,y2)}.
     * Решает t методом Ньютона затем вычисляет Y.
     */
    public static float cubicBezier(float t, float x1, float y1, float x2, float y2) {
        t=clamp(t);
        float u = t;
        for(int i=0; i<8; i++) {
            float bx = bezierCoord(u, x1, x2) - t;
            float dx = bezierDeriv(u, x1, x2);
            if(Math.abs(dx) < 1e-6f) break;
            u -= bx / dx;
        }
        return bezierCoord(u, y1, y2);
    }

    private static float bezierCoord(float t, float p1, float p2) {
        return 3*(1-t)*(1-t)*t*p1 + 3*(1-t)*t*t*p2 + t*t*t;
    }
    private static float bezierDeriv(float t, float p1, float p2) {
        return 3*(1-t)*(1-t)*p1 + 6*(1-t)*t*(p2-p1) + 3*t*t*(1-p2);
    }

    /**
     * Preset cubic-bezier curves matching CSS easings.
     * <p>
     * Предустановленные кубические bezier кривые соответствующие CSS easing.
     */
    public static float cssEase      (float t) { return cubicBezier(t, 0.25f,0.1f,0.25f,1.0f); }
    public static float cssEaseIn    (float t) { return cubicBezier(t, 0.42f,0f,  1.0f, 1.0f); }
    public static float cssEaseOut   (float t) { return cubicBezier(t, 0f,   0f,  0.58f,1.0f); }
    public static float cssEaseInOut (float t) { return cubicBezier(t, 0.42f,0f,  0.58f,1.0f); }

    /**
     * Quantizes t into {@code steps} discrete steps.
     * <p>
     * Квантует t в {@code steps} дискретных шагов.
     */
    public static float stepped(float t, int steps) {
        t=clamp(t);
        return (float)Math.floor(t * steps) / steps;
    }

    /**
     * Mirrors the easing: in→out becomes out→in.
     * <p>
     * Зеркалит easing: in→out становится out→in.
     */
    public static float mirror(float t, java.util.function.Function<Float,Float> fn) {
        return t < 0.5f ? fn.apply(t*2)/2 : 1 - fn.apply((1-t)*2)/2;
    }

    /**
     * Applies easing twice (sharpens the curve).
     * <p>
     * Применяет easing дважды (заостряет кривую).
     */
    public static float squared(float t, java.util.function.Function<Float,Float> fn) {
        float v = fn.apply(t); return v*v;
    }
}
