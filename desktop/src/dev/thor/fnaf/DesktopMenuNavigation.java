package dev.thor.fnaf;
/** Menu-only threshold and repeat delay; small look-stick drift must not select rows. */
final class DesktopMenuNavigation {
    private int direction;
    private long repeatAt;
    void reset(){direction=0;repeatAt=0;}
    int step(float axis,long now){
        if(Math.abs(axis)<.30f){reset();return 0;}
        int next=axis<0?-1:1;
        if(next!=direction){
            if(Math.abs(axis)<.55f)return 0;
            direction=next;repeatAt=now+350;return next;
        }
        if(now<repeatAt)return 0;
        repeatAt=now+180;return direction;
    }
}
