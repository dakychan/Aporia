import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;

import java.util.Arrays;

public class A {
  static   Animation animation = new Animation(200, Easing.LINEAR);
    static Animation animation2 = new Animation(200, Easing.LINEAR);
    static Animation animation3 = new Animation(200, Easing.LINEAR);
    static Animation animation4 = new Animation(200,1, Easing.LINEAR);

    public static void main(String[] args) {
        DeltaCycle deltaCycle = new DeltaCycle(400);
        while (true){
            deltaCycle.update();
            System.out.println(Arrays.toString(deltaCycle.getDeltas()));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
        }
    }
}
