package android.view;
public class InputDevice {public static final int SOURCE_GAMEPAD=0x401,SOURCE_JOYSTICK=0x1000010;public MotionRange getMotionRange(int a,int s){return new MotionRange();}public MotionRange getMotionRange(int a){return new MotionRange();}public static class MotionRange{public float getFlat(){return .18f;}}}
