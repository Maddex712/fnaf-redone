package dev.thor.fnaf;

import android.app.Activity;
import android.app.Presentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity implements DisplayManager.DisplayListener, InputManager.InputDeviceListener {
    static final double MENU_REVEAL_DURATION=3.0,MENU_REVEAL_HOLD=.45;
    final GameState state = new GameState();
    final NightIntro intro=new NightIntro();
    final NightOutro outro=new NightOutro();
    int[] pendingCustom;
    final Handler handler = new Handler(Looper.getMainLooper());
    DisplayManager displays;
    SharedPreferences prefs;
    PanelView primary, secondary;
    Presentation presentation;
    GameAssets assets;
    GameAudio audio;
    boolean swapped, resumed, diagnostic, showHelp;
    boolean customizing, savedWin, options;
    boolean dashboard;
    int screenMode=ScreenMode.AUTO;
    Progress progress;
    int journal, activeChallenge=-1, campaignNight=1;
    String achievementNotice="",noticeTitle="ACHIEVEMENT UNLOCKED",shopMessage="";
    int buyConfirm=-1,runPoints;
    long shopMessageUntil,menuEpoch=SystemClock.uptimeMillis();
    long noticeUntil;
    int sensitivity=2;
    static final float[] LOOK_SPEEDS={1.6f,2.6f,3.8f,5.2f};
    static final String[] SENSITIVITIES={"Low","Medium","High","Very high"};
    int customSelected, unlocked=1;
    int[] bindingBackup;
    int selected, selectedDisplay = -1, bindIndex = -1;
    int[] keys = {KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT};
    static final String[] ACTIONS = {"Left door", "Right door", "Left light", "Right light", "Monitor", "Previous camera", "Next camera"};
    String lastInput = "Waiting for controller input", displayStatus = "Detecting displays";
    float lastHat;
    long lastNavigation, lastFrame;
    boolean triggerLeft, triggerRight;
    final NightSkipChord nightSkip=new NightSkipChord();
    final NightSkipChord jumpscareChord=new NightSkipChord(7);

    void openProjectPage(){
        try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/MjonirMade/fnaf-redone")));}
        catch(android.content.ActivityNotFoundException e){new android.app.AlertDialog.Builder(this).setMessage("No browser is installed. Visit github.com/MjonirMade/fnaf-redone.").setPositiveButton("OK",null).show();}
    }
    AndroidUpdater updater;
    void checkForUpdates(){updater.check();}
    String versionLabel(){return "FNaF Redone "+updater.version();}
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        updater=new AndroidUpdater(this);
        prefs = getSharedPreferences("thor", MODE_PRIVATE);
        screenMode=ScreenMode.valid(prefs.getInt("screen_mode",ScreenMode.AUTO));
        progress=new Progress(prefs.getString("progress_v1",null));
        saveProgress();
        sensitivity=Math.max(0,Math.min(3,prefs.getInt("sensitivity",2)));state.lookSpeed=LOOK_SPEEDS[sensitivity];
        assets=new GameAssets(this);audio=new GameAudio(assets);assets.warm();
        unlocked=prefs.getInt("unlocked",1);state.night=Math.min(6,unlocked);
        swapped = prefs.getBoolean("swapped", false);
        for (int i=0;i<keys.length;i++) keys[i] = prefs.getInt("key"+i, keys[i]);
        fullscreen(getWindow());
        displays = (DisplayManager)getSystemService(DISPLAY_SERVICE);
        displays.registerDisplayListener(this, handler);
        ((InputManager)getSystemService(INPUT_SERVICE)).registerInputDeviceListener(this, handler);
        primary = new PanelView(this, this, true);
        setContentView(primary);
    }
    static void fullscreen(Window window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams p = window.getAttributes();
            p.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(p);
        }
    }
    @Override protected void onResume() {
        super.onResume(); if(outro.active()){state.paused=false;audio.pause(false);} resumed = true; connectDisplay(); lastFrame = SystemClock.elapsedRealtimeNanos(); handler.post(frame);
        fullscreen(getWindow());
    }
    @Override protected void onPause() {
        resumed = false; handler.removeCallbacks(frame); state.pause(); audio.pause(true); releaseInputs(); dismissDisplay(); super.onPause();
    }
    @Override protected void onDestroy() {
        updater.close();
        handler.removeCallbacksAndMessages(null); displays.unregisterDisplayListener(this);
        ((InputManager)getSystemService(INPUT_SERVICE)).unregisterInputDeviceListener(this);
        dismissDisplay(); audio.stopAll();assets.close();super.onDestroy();
    }
    final Runnable frame = new Runnable() { public void run() {
        if (!resumed) return;
        long now = SystemClock.elapsedRealtimeNanos();double dt=(now-lastFrame)/1e9;lastFrame=now;
        if(menuReveal>0)menuReveal=Math.max(0,menuReveal-Math.min(.25,Math.max(0,dt)));
        if(outro.active()){
            int events=outro.tick(dt,audio.victoryFinished());
            if((events&NightOutro.CHEER)!=0)audio.play("cheer","CROWD_SMALL_CHIL_EC049202",false,.75f);
            if((events&NightOutro.SHOW_ENDING)!=0){audio.stopAll();audio.play("ending","music box",true,.65f);}
            if((events&NightOutro.DONE)!=0)completeOutro();
        }else if(intro.active()){
            int previous=intro.stage;
            if(intro.tick(dt))beginGameplay();
            else if(previous!=intro.stage&&intro.stage==2)audio.channelSwitch();
        }else state.tick(dt);
        if(state.running)audio.update(state);
        else if(!intro.active()){
            audio.titleMenu();
            audio.volume("title",.55f*menuRevealProgress());
        }
        if(state.dead){
            deathElapsed+=Math.min(.25,Math.max(0,dt));
            if(deathElapsed>=5){finishResult();menuReveal=MENU_REVEAL_DURATION;audio.channelSwitch();}
        }else deathElapsed=0;
        int awards=progress.update(state);
        if(awards!=0||progress.lastPoints>0){
            runPoints+=progress.lastPoints;
            noticeTitle=awards!=0?"ACHIEVEMENT UNLOCKED":"ASSIGNMENT COMPLETE";
            achievementNotice=(awards!=0?Progress.TITLES[Integer.numberOfTrailingZeros(awards)]:"Shift survived")+"  +"+progress.lastPoints+" pts";
            noticeUntil=SystemClock.uptimeMillis()+6500;
            prefs.edit().putString("progress_v1",progress.save()).apply();
        }
        if(state.won&&!savedWin){
            savedWin=true;outro.begin(state.night,activeChallenge<0);releaseInputs();
            if(activeChallenge<0&&state.night<=6)unlocked=Math.max(unlocked,Math.min(7,state.night+1));
            prefs.edit().putInt("unlocked",unlocked).putString("progress_v1",progress.save()).apply();
        }
        primary.invalidate(); if (secondary != null) secondary.invalidate(); handler.postDelayed(this,16);
    }};
    void dismissDisplay() { if (presentation != null) { presentation.dismiss(); presentation = null; } secondary = null; }
    ArrayList<Display> candidates() {
        int current = primary.getDisplay() != null ? primary.getDisplay().getDisplayId() : Display.DEFAULT_DISPLAY;
        ArrayList<Display> result = new ArrayList<>();
        for (Display d : displays.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)) if (d.getDisplayId() != current) result.add(d);
        result.sort((a,b)-> {
            boolean aInternal = a.getName().equalsIgnoreCase("Screen-2"), bInternal = b.getName().equalsIgnoreCase("Screen-2");
            return aInternal != bInternal ? (aInternal ? -1 : 1) : Integer.compare(a.getDisplayId(),b.getDisplayId());
        });
        return result;
    }
    void connectDisplay() {
        if (!resumed) return;
        ArrayList<Display> choices = candidates();
        if (!ScreenMode.useSecondDisplay(screenMode,!choices.isEmpty())) {
            dismissDisplay();displayStatus=screenMode==ScreenMode.SINGLE?"Single-screen mode":"One display • full-screen office / cameras";return;
        }
        Display target = choices.get(0);
        for (Display d : choices) if (d.getDisplayId() == selectedDisplay) target = d;
        if (presentation != null && presentation.getDisplay().getDisplayId() == target.getDisplayId()) return;
        dismissDisplay(); selectedDisplay = target.getDisplayId();
        try {
            presentation = new Presentation(this, target, android.R.style.Theme_Material_NoActionBar_Fullscreen) {
                @Override public boolean dispatchKeyEvent(KeyEvent e) { return handleKey(e) || super.dispatchKeyEvent(e); }
                @Override public boolean dispatchGenericMotionEvent(MotionEvent e) { return handleMotion(e) || super.dispatchGenericMotionEvent(e); }
            };
            secondary = new PanelView(presentation.getContext(), this, false);
            presentation.setContentView(secondary); presentation.setCancelable(false); presentation.show(); fullscreen(presentation.getWindow());
            displayStatus = "Dual display • " + target.getName() + " (#" + target.getDisplayId() + ")";
        } catch (WindowManager.InvalidDisplayException | SecurityException e) {
            dismissDisplay(); displayStatus = "Display unavailable • single-screen mode";
        }
    }
    @Override public void onDisplayAdded(int id) { connectDisplay(); }
    @Override public void onDisplayRemoved(int id) { state.pause(); releaseInputs(); connectDisplay(); }
    @Override public void onDisplayChanged(int id) { connectDisplay(); }
    @Override public void onInputDeviceAdded(int id) { lastInput = "Controller connected: " + id; }
    @Override public void onInputDeviceRemoved(int id) { releaseInputs(); state.pause(); lastInput = "Controller disconnected • paused"; }
    @Override public void onInputDeviceChanged(int id) { releaseInputs(); }
    void releaseInputs() { state.clearHeld(); triggerLeft = triggerRight = false; lastHat = 0; nightSkip.reset(); jumpscareChord.reset(); }
    boolean menu() { return !state.running || state.paused; }
    String[] menuItems() {
        if(dashboard)return new String[]{state.running?"Resume":"Start night","Achievements","Challenges","Office Shop","Back"};
        if(journal!=0){
            String[] names=journal==1?Progress.CHALLENGES:journal==3?Progress.PLUSHIES:Progress.TITLES;
            String[] rows=new String[names.length+1];
            for(int i=0;i<names.length;i++)rows[i]=journal==3?names[i]+(progress.wearing(i)?"  [ON]":progress.owns(i)?"  [OWNED]":"  "+Progress.PRICES[i]+" pts"):
                (journal==1?progress.cleared(i):progress.earned(i))?names[i]+"  *":names[i];
            rows[names.length]="Back";return rows;
        }
        if(options)return new String[]{"Screen mode: "+ScreenMode.LABELS[screenMode],"Shift dashboard","Look sensitivity: "+SENSITIVITIES[sensitivity],"Swap screens","Second display","Controller check","Remap buttons","Controls","Import Steam file","Import local font","Check for updates","GitHub project","Back"};
        if(state.running)return new String[]{"Resume","Office Shop","Options","Quit to title"};
        return new String[]{state.night==1?"New Game":"Continue","Night "+state.night+(state.night==7?" / Custom":""),"Challenges","Achievements","Office Shop","Options","Exit"};
    }
    int[] menuActions(){return dashboard?new int[]{0,14,13,15,18}:options?new int[]{17,18,10,2,3,4,5,6,7,16,19,21,11}:state.running?new int[]{0,15,12,8}:new int[]{0,1,13,14,15,12,9};}
    void startNight(){if(!assets.ready||assets.importing){showHelp=true;return;}savedWin=false;runPoints=0;releaseInputs();pendingCustom=state.custom.clone();
        intro.begin(state.night==1&&activeChallenge<0);audio.stopAll();audio.pause(false);
        if(intro.stage==2)audio.menuBlip();assets.warm();}
    double deathElapsed,menuReveal;
    float menuRevealProgress(){
        if(menuReveal<=0)return 1;
        double age=MENU_REVEAL_DURATION-menuReveal;
        return age<=MENU_REVEAL_HOLD?0:TitleEffects.smooth((float)((age-MENU_REVEAL_HOLD)/(MENU_REVEAL_DURATION-MENU_REVEAL_HOLD)));
    }
    void beginGameplay(){
        int[] original=state.custom;state.custom=pendingCustom;state.start();state.sounds.add("@channelSwitch");state.custom=original;
        pendingCustom=null;deathElapsed=menuReveal=0;progress.begin(state,activeChallenge);lastFrame=SystemClock.elapsedRealtimeNanos();
    }
    void navigate(int delta){if(buyConfirm>=0)return;shopMessage="";selected=Math.floorMod(selected+delta,menuItems().length);audio.menuBlip();}
    void skipNewspaper(){int previous=intro.stage;intro.skipPaper();if(previous!=intro.stage)audio.menuBlip();}
    void activate(int item) {
        if(buyConfirm>=0){confirmPurchase();return;}
        audio.menuBlip();
        if(journal!=0){
            if(item==menuItems().length-1){back();return;}
            selected=item;
            if(journal==1)startChallenge(item);else if(journal==3)shopAction(item);
            return;
        }
        selected = item;
        item=menuActions()[item];
        if(dashboard&&item!=18)dashboard=false;
        switch (item) {
            case 0: if(!assets.ready||assets.importing){showHelp=true;break;}if(!state.running){if(state.night==7)customizing=true;else startNight();}else{state.paused=false;releaseInputs();}break;
            case 1: if(!state.running)state.night=state.night%unlocked+1;break;
            case 2: swapped = !swapped; prefs.edit().putBoolean("swapped", swapped).apply(); break;
            case 3:
                ArrayList<Display> choices = candidates();
                if (!choices.isEmpty()) { int index=0; for(int i=0;i<choices.size();i++) if(choices.get(i).getDisplayId()==selectedDisplay) index=i;
                    selectedDisplay = choices.get((index+1)%choices.size()).getDisplayId(); dismissDisplay(); connectDisplay(); }
                break;
            case 4: diagnostic = true; break;
            case 5: bindingBackup=keys.clone();bindIndex = 0; releaseInputs(); break;
            case 6: showHelp = true; break;
            case 7: chooseLocalFile(10);break;
            case 8:finishResult();break;
            case 9: finish(); break;
            case 10:sensitivity=(sensitivity+1)%LOOK_SPEEDS.length;state.lookSpeed=LOOK_SPEEDS[sensitivity];prefs.edit().putInt("sensitivity",sensitivity).apply();break;
            case 11:closeSubmenu();break;
            case 12:openOptions();break;
            case 13:openJournal(1);break;
            case 14:openJournal(2);break;
            case 15:openJournal(3);break;
            case 16:chooseLocalFile(11);break;
            case 17:
                screenMode=(screenMode+1)%ScreenMode.LABELS.length;
                prefs.edit().putInt("screen_mode",screenMode).apply();releaseInputs();connectDisplay();break;
            case 21:openProjectPage();break;
            case 19:checkForUpdates();break;
            case 18:
                if(dashboard){dashboard=false;options=true;selected=1;}
                else{dashboard=true;options=false;selected=0;}
                break;
        }
    }
    int menuReturnSelection;
    int nextObjective(){for(int i=0;i<Progress.TITLES.length;i++)if(!progress.earned(i))return i;return -1;}
    int nextChallenge(){for(int i=0;i<Progress.CHALLENGES.length;i++)if(!progress.cleared(i))return i;return 0;}
    long lastBoop=-1000;
    void boop(){
        long now=SystemClock.uptimeMillis();
        if(state.active()&&!state.monitor&&state.powerStage==0&&now-lastBoop>=120){
            lastBoop=now;audio.play("nose","PartyFavorraspyPart_AC01__3",false,.75f);
        }
    }
    void openOptions(){dashboard=false;menuReturnSelection=selected;options=true;selected=0;}
    void closeSubmenu(){dashboard=false;journal=0;options=false;selected=Math.min(menuReturnSelection,menuItems().length-1);}
    void openJournal(int page){dashboard=false;menuReturnSelection=selected;journal=page;selected=0;options=false;buyConfirm=-1;shopMessage="";}
    void saveProgress(){prefs.edit().putString("progress_v1",progress.save()).apply();}
    void shopAction(int i){
        if(progress.owns(i)){progress.togglePlush(i);saveProgress();shopMessage=progress.wearing(i)?"Placed on your desk":"Stored in your collection";}
        else if(progress.points>=Progress.PRICES[i])buyConfirm=i;
        else shopMessage="Need "+(Progress.PRICES[i]-progress.points)+" more points";
        shopMessageUntil=SystemClock.uptimeMillis()+3500;
    }
    void confirmPurchase(){
        int i=buyConfirm;buyConfirm=-1;if(i<0)return;
        if(progress.buy(i)){saveProgress();audio.menuBlip();shopMessage=Progress.PLUSHIES[i]+" purchased & placed";}
        else shopMessage="Purchase unavailable";
        shopMessageUntil=SystemClock.uptimeMillis()+3500;
    }
    void startChallenge(int id){
        if(!assets.ready||assets.importing){showHelp=true;return;}
        campaignNight=state.night;activeChallenge=id;state.night=7;
        int[] custom=state.custom.clone();state.custom=Progress.LEVELS[id].clone();
        journal=0;selected=0;startNight();state.custom=custom;
    }
    void back() {
        if(menuReveal>0)return;
        if(state.dead)return;
        if(outro.active()){if(outro.skipEnding())completeOutro();return;}
        if(buyConfirm>=0){buyConfirm=-1;audio.menuBlip();return;}
        if(intro.active()){
            intro.cancel();pendingCustom=null;
            if(activeChallenge>=0)state.night=campaignNight;
            activeChallenge=-1;state.paused=false;selected=0;audio.menuBlip();return;
        }
        if(menu())audio.menuBlip();
        if(dashboard){dashboard=false;options=true;selected=1;return;}
        if (bindIndex >= 0) { keys=bindingBackup;bindIndex = -1; return; }
        if(customizing){customizing=false;return;}
        if (diagnostic || showHelp) { diagnostic = showHelp = false; return; }
        if(journal!=0){closeSubmenu();return;}
        if(options){closeSubmenu();return;}
        if(state.running&&(state.dead||state.won)){finishResult();return;}
        if (state.running) { if (state.paused) state.paused = false; else state.pause(); releaseInputs(); }
    }
    @Override public void onBackPressed() { back(); }
    void chooseLocalFile(int request){
        if(assets.importing)return;
        state.pause();releaseInputs();
        Intent choose=new Intent(Intent.ACTION_OPEN_DOCUMENT);choose.setType("*/*");
        choose.addCategory(Intent.CATEGORY_OPENABLE);
        try{startActivityForResult(choose,request);}catch(android.content.ActivityNotFoundException e){
            new android.app.AlertDialog.Builder(this).setMessage("A system file picker is required. Enable the Files app and try again.").setPositiveButton("OK",null).show();
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(updater.activityResult(request))return;
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        if(request==10){
            audio.stopAll();state.running=false;state.paused=false;showHelp=false;
            assets.importPack(data.getData(),()->runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                assets.warm();primary.invalidate();
                new android.app.AlertDialog.Builder(this).setTitle(assets.status.startsWith("Import failed")?"Import failed":"Ready to play")
                    .setMessage(assets.status.startsWith("Import failed")?assets.status:"Your Steam data is stored on this device. You can start Night 1 from the title menu.")
                    .setPositiveButton("OK",null).show();
            }));
        }else if(request==11){
            java.io.File staged=new java.io.File(getFilesDir(),"font-import.tmp");
            try{
                try(java.io.InputStream in=getContentResolver().openInputStream(data.getData());java.io.OutputStream out=new java.io.FileOutputStream(staged)){
                    if(in==null)throw new java.io.IOException("Cannot open font");
                    byte[] buffer=new byte[8192];int n,total=0;
                    while((n=in.read(buffer))!=-1){total+=n;if(total>4*1024*1024)throw new java.io.IOException("Select a TTF font smaller than 4 MB");out.write(buffer,0,n);}
                }
                // Validate before replacing a previously working font.
                android.graphics.Typeface face=android.graphics.Typeface.createFromFile(staged);
                java.nio.file.Files.move(staged.toPath(),new java.io.File(getFilesDir(),"menu-font.ttf").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                PanelView.menuFont=face;primary.p.setTypeface(face);if(secondary!=null)secondary.p.setTypeface(face);
                new android.app.AlertDialog.Builder(this).setMessage("Local menu font imported.").setPositiveButton("OK",null).show();
            }catch(Exception e){
                new android.app.AlertDialog.Builder(this).setMessage("Font import failed. Select your local consola.ttf file. Your previous font is unchanged.").setPositiveButton("OK",null).show();
            }finally{staged.delete();}
        }
    }
    @Override public boolean dispatchKeyEvent(KeyEvent e) { return handleKey(e) || super.dispatchKeyEvent(e); }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent e) { return handleMotion(e) || super.dispatchGenericMotionEvent(e); }
    boolean handleKey(KeyEvent e) {
        int k=e.getKeyCode(); boolean down=e.getAction()==KeyEvent.ACTION_DOWN;
        if (k==KeyEvent.KEYCODE_VOLUME_UP || k==KeyEvent.KEYCODE_VOLUME_DOWN || k==KeyEvent.KEYCODE_POWER) return false;
        if(assets.importing)return true;
        lastInput = KeyEvent.keyCodeToString(k) + (down ? " ↓" : " ↑") + " • device " + e.getDeviceId();
        if(menuReveal>0)return true;
        if (k==KeyEvent.KEYCODE_BACK || k==KeyEvent.KEYCODE_ESCAPE || k==KeyEvent.KEYCODE_BUTTON_START) { if(down && e.getRepeatCount()==0) back(); return true; }
        if(outro.active()){
            if(down&&e.getRepeatCount()==0&&(k==KeyEvent.KEYCODE_BUTTON_A||k==KeyEvent.KEYCODE_BUTTON_B||k==KeyEvent.KEYCODE_ENTER)){
                if(outro.skipEnding())completeOutro();
            }return true;
        }
        if(intro.active()){
            if(down&&e.getRepeatCount()==0){if(k==KeyEvent.KEYCODE_BUTTON_B)back();
                else if(k==KeyEvent.KEYCODE_BUTTON_A||k==KeyEvent.KEYCODE_ENTER||k==KeyEvent.KEYCODE_DPAD_CENTER)skipNewspaper();}
            return true;
        }
        if (bindIndex>=0) {
            if(down && e.getRepeatCount()==0) {
                int previous = keys[bindIndex];
                for(int i=0;i<keys.length;i++) if(i!=bindIndex && keys[i]==k) keys[i]=previous;
                keys[bindIndex++]=k;
                if(bindIndex==keys.length) { bindIndex=-1; SharedPreferences.Editor ed=prefs.edit(); for(int i=0;i<keys.length;i++) ed.putInt("key"+i,keys[i]); ed.apply(); }
            } return true;
        }
        if(diagnostic || showHelp) {
            if(down&&e.getRepeatCount()==0){
                if(k==KeyEvent.KEYCODE_BUTTON_B)back();
                else if(showHelp&&!assets.ready&&(k==KeyEvent.KEYCODE_BUTTON_A||k==KeyEvent.KEYCODE_ENTER||k==KeyEvent.KEYCODE_DPAD_CENTER))chooseLocalFile(10);
            }return true;
        }
        if(customizing){if(down){if(k==KeyEvent.KEYCODE_BUTTON_B){customizing=false;return true;}if(k==KeyEvent.KEYCODE_DPAD_UP)customSelected=Math.floorMod(customSelected-1,5);if(k==KeyEvent.KEYCODE_DPAD_DOWN)customSelected=(customSelected+1)%5;if(customSelected<4&&(k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT))state.custom[customSelected]=Math.max(0,Math.min(20,state.custom[customSelected]+(k==KeyEvent.KEYCODE_DPAD_LEFT?-1:1)));if((k==KeyEvent.KEYCODE_BUTTON_A||k==KeyEvent.KEYCODE_ENTER)&&customSelected==4){customizing=false;startNight();}}return true;}
        if(state.dead)return true;
        if(state.won){if(down&&e.getRepeatCount()==0&&(k==KeyEvent.KEYCODE_BUTTON_A||k==KeyEvent.KEYCODE_ENTER)){finishResult();}return true;}
        if(dashboard&&!state.running&&(k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT)){
            if(down&&e.getRepeatCount()==0){state.night=Math.floorMod(state.night-1+(k==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1),unlocked)+1;audio.menuBlip();}
            return true;
        }
        if(menu()) {
            if(down) {
                if(k==KeyEvent.KEYCODE_BUTTON_B){if(e.getRepeatCount()==0)back();return true;}
                if(k==KeyEvent.KEYCODE_DPAD_UP||(journal!=0&&k==KeyEvent.KEYCODE_DPAD_LEFT)) navigate(-1);
                if(k==KeyEvent.KEYCODE_DPAD_DOWN||(journal!=0&&k==KeyEvent.KEYCODE_DPAD_RIGHT)) navigate(1);
                if((k==KeyEvent.KEYCODE_BUTTON_A || k==KeyEvent.KEYCODE_ENTER || k==KeyEvent.KEYCODE_DPAD_CENTER) && e.getRepeatCount()==0) activate(selected);
            } return true;
        }
        int secretBit=k==KeyEvent.KEYCODE_BUTTON_L1?NightSkipChord.L1:k==KeyEvent.KEYCODE_BUTTON_R1?NightSkipChord.R1:
            k==KeyEvent.KEYCODE_BUTTON_L2?NightSkipChord.L2:k==KeyEvent.KEYCODE_BUTTON_R2?NightSkipChord.R2:
            k==KeyEvent.KEYCODE_BUTTON_A?NightSkipChord.A:k==KeyEvent.KEYCODE_BUTTON_B?NightSkipChord.B:0;
        if(!state.active())nightSkip.reset();
        else if(ReleaseConfig.DEVELOPER_SHORTCUTS&&secretBit!=0&&nightSkip.button(secretBit,down)){state.skipToLastFiveSeconds();return true;}
        int jumpBit=k==KeyEvent.KEYCODE_BUTTON_THUMBL?1:k==KeyEvent.KEYCODE_BUTTON_THUMBR?2:k==KeyEvent.KEYCODE_BUTTON_Y?4:0;
        if(!state.active())jumpscareChord.reset();
        else if(ReleaseConfig.DEVELOPER_SHORTCUTS&&jumpBit!=0&&jumpscareChord.button(jumpBit,down)){
            state.triggerRandomJumpscare();releaseInputs();return true;
        }
        if(k==KeyEvent.KEYCODE_BUTTON_THUMBR){if(down&&e.getRepeatCount()==0)boop();return true;}
        for(int i=0;i<keys.length;i++) if(k==keys[i]) {
            if(i==2 || i==3) state.light(i==2,down);
            else if(down && e.getRepeatCount()==0) action(i);
            return true;
        }
        if(k==KeyEvent.KEYCODE_BUTTON_Y){if(down)state.mutedCall=true;return true;}
        if(k==KeyEvent.KEYCODE_A || k==KeyEvent.KEYCODE_D) { state.axis=down?(k==KeyEvent.KEYCODE_A?-1:1):0; return true; }
        // Unassigned game buttons must not invoke Android's full-view focus/click behavior.
        return k==KeyEvent.KEYCODE_BUTTON_A || k==KeyEvent.KEYCODE_ENTER || k==KeyEvent.KEYCODE_DPAD_CENTER
            || (e.getSource() & InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD;
    }
    void action(int i) { switch(i) { case 0:state.toggleDoor(true);break; case 1:state.toggleDoor(false);break; case 4:state.toggleMonitor();break; case 5:state.selectCamera(state.camera-1);break; case 6:state.selectCamera(state.camera+1);break; } }
    float axis(MotionEvent e,int a) {
        float x=e.getAxisValue(a); InputDevice d=e.getDevice(); InputDevice.MotionRange r=d==null?null:d.getMotionRange(a,e.getSource());
        return Math.abs(x)>Math.max(.18f,r==null?0:r.getFlat())?x:0;
    }
    boolean handleMotion(MotionEvent e) {
        if((e.getSource() & InputDevice.SOURCE_JOYSTICK)!=InputDevice.SOURCE_JOYSTICK) return false;
        float x=axis(e,MotionEvent.AXIS_X), y=axis(e,MotionEvent.AXIS_Y);
        float hx=axis(e,MotionEvent.AXIS_HAT_X), hy=axis(e,MotionEvent.AXIS_HAT_Y);
        float lt=Math.max(e.getAxisValue(MotionEvent.AXIS_LTRIGGER),e.getAxisValue(MotionEvent.AXIS_BRAKE));
        float rt=Math.max(e.getAxisValue(MotionEvent.AXIS_RTRIGGER),e.getAxisValue(MotionEvent.AXIS_GAS));
        lastInput=String.format(Locale.US,"Stick %+.2f, %+.2f   LT %.2f  RT %.2f",x,y,lt,rt);
        if(intro.active()||outro.active())return true;
        if(menu() || diagnostic || bindIndex>=0 || showHelp || customizing) {
            if(menu() && !diagnostic && bindIndex<0 && !showHelp && !customizing) {
                float nav=hy!=0?hy:y; long now=SystemClock.uptimeMillis();
                if(nav!=0 && now-lastNavigation>220) { navigate(nav>0?1:-1);lastNavigation=now; }
            } return true;
        }
        if(!state.active())nightSkip.reset();
        else if(ReleaseConfig.DEVELOPER_SHORTCUTS&&nightSkip.triggers(lt,rt)){state.skipToLastFiveSeconds();return true;}
        state.axis=x;
        if(hx!=0 && lastHat==0) state.selectCamera(state.camera+(hx>0?1:-1)); lastHat=hx;
        // Axis triggers are used only when their actions retain default trigger bindings.
        InputDevice pad=e.getDevice();
        if(pad!=null && (pad.getMotionRange(MotionEvent.AXIS_LTRIGGER)!=null||pad.getMotionRange(MotionEvent.AXIS_BRAKE)!=null) && keys[2]==KeyEvent.KEYCODE_BUTTON_L2 && (lt>.25f)!=triggerLeft) {triggerLeft=lt>.25f;state.light(true,triggerLeft);}
        if(pad!=null && (pad.getMotionRange(MotionEvent.AXIS_RTRIGGER)!=null||pad.getMotionRange(MotionEvent.AXIS_GAS)!=null) && keys[3]==KeyEvent.KEYCODE_BUTTON_R2 && (rt>.25f)!=triggerRight) {triggerRight=rt>.25f;state.light(false,triggerRight);}
        return true;
    }
    String diagnostics() {
        StringBuilder b=new StringBuilder(displayStatus).append('\n');
        for(Display d:displays.getDisplays()) b.append("#").append(d.getDisplayId()).append(" ").append(d.getName()).append(" ").append(d.getMode().getPhysicalWidth()).append("×").append(d.getMode().getPhysicalHeight()).append(" @").append(Math.round(d.getRefreshRate())).append("Hz\n");
        for(int id:InputDevice.getDeviceIds()) { InputDevice d=InputDevice.getDevice(id); if(d!=null && ((d.getSources()&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD || (d.getSources()&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK)) b.append("Pad: ").append(d.getName()).append('\n'); }
        return b.append("\n").append(assets.status).append("\n").append(audio.lastError).toString();
    }
    void completeOutro(){
        boolean next=outro.campaign&&outro.night<5;int nextNight=outro.night+1;
        outro.cancel();finishResult();
        if(next){state.night=nextNight;startNight();intro.beginNextNight();audio.channelSwitch();}
    }
    void finishResult(){if(outro.active()){if(outro.skipEnding())completeOutro();return;}int returnNight=activeChallenge>=0?campaignNight:Math.min(6,unlocked);progress.abandon();activeChallenge=-1;audio.stopAll();state.running=false;state.paused=false;state.dead=false;state.won=false;state.night=returnNight;deathElapsed=0;selected=0;}
}



