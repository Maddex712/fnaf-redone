package dev.thor.fnaf;

/** Re-evaluated from the current display size on launch and whenever it changes. */
final class DisplayLayout {
    static float[] fit(int width,int height){
        if(width<=0||height<=0)return new float[]{0,0,0,0};
        float w=Math.min(width,height*16f/9f),h=w*9f/16f;
        return new float[]{(width-w)/2,(height-h)/2,w,h};
    }
    static String describe(int width,int height){
        if(width<=0||height<=0)return "Detecting display";
        int a=width,b=height;while(b!=0){int r=a%b;a=b;b=r;}
        String ratio=(width/a)+":"+(height/a);
        float actual=(float)width/height;
        if(Math.abs(actual-16f/9)<.012)ratio="16:9";
        else if(Math.abs(actual-16f/10)<.012)ratio="16:10";
        else if(Math.abs(actual-19f/10)<.012)ratio="19:10";
        else if(Math.abs(actual-21f/9)<.012)ratio="21:9";
        return width+" x "+height+" / "+ratio;
    }
}
