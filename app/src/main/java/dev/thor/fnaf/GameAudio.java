package dev.thor.fnaf;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import java.io.File;
import java.util.HashMap;

/** All output is local. Each long track has a stable channel and pause lifecycle. */
final class GameAudio {
    final GameAssets assets;
    final HashMap<String,MediaPlayer> channels=new HashMap<>();
    boolean paused;
    String lastError="";
    GameAudio(GameAssets a){assets=a;}
    void play(String channel,String name,boolean loop,float volume){
        play(channel,name,loop,volume,0);
    }
    void play(String channel,String name,boolean loop,float volume,int durationMs){
        if(!assets.ready)return;stop(channel);
        File f=new File(assets.directory,"Sounds/"+name+".wav");if(!f.isFile()){lastError="Missing sound: "+name;return;}
        MediaPlayer p=new MediaPlayer();channels.put(channel,p);
        try{
            p.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            p.setDataSource(f.getPath());p.setLooping(loop);p.setVolume(volume,volume);
            p.setOnPreparedListener(m->{if(channels.get(channel)==m&&!paused){
                m.start();
                if(durationMs>0)new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{if(channels.get(channel)==m)stop(channel);},durationMs);
            }});
            p.setOnCompletionListener(m->{if(!loop&&channels.get(channel)==m)stop(channel);});
            p.setOnErrorListener((m,w,e)->{lastError="Audio decode failed: "+name;if(channels.get(channel)==m)stop(channel);return true;});
            p.prepareAsync();
        }catch(Exception e){lastError=e.getMessage();stop(channel);}
    }
    void stop(String c){MediaPlayer p=channels.remove(c);if(p!=null)p.release();}
    void stopAll(){for(MediaPlayer p:channels.values())p.release();channels.clear();}
    boolean victoryFinished(){return !channels.containsKey("win")&&!channels.containsKey("cheer");}
    void pause(boolean value){if(paused==value)return;paused=value;for(MediaPlayer p:channels.values())try{if(value)p.pause();else p.start();}catch(IllegalStateException ignored){}}
    void volume(String c,float v){MediaPlayer p=channels.get(c);if(p!=null)try{p.setVolume(v,v);}catch(IllegalStateException ignored){}}
    void titleMenu(){
        pause(false);
        if(!channels.containsKey("title"))play("title","darkness music",true,.55f);
    }
    void menuBlip(){play("menuSelect","blip3",false,.45f);}
    void channelSwitch(){play("introSwitch","blip3",false,.45f);}
    void update(GameState s){
        pause(s.paused);
        while(!s.sounds.isEmpty()){
            String event=s.sounds.remove();
            switch(event){
                case "@channelSwitch":channelSwitch();break;
                case "@start":stopAll();play("fan","Buzz_Fan_Florescent2",true,.25f);play("ambient","ColdPresc B",true,.5f);play("light","BallastHumMedium2",true,0);
                    if(s.night<=5)play("phone",new String[]{"voiceover1c","voiceover2a","voiceover3","voiceover4","voiceover5"}[s.night-1],false,.8f);break;
                case "@power":stopAll();play("power","powerdown",false,.7f);play("ambient","ambience2",true,.5f);break;
                case "@blackout":stopAll();break;
                case "@kitchenMusic":play("kitchenMusic","music box",false,s.kitchenMusicVolume());break;
                case "@scare":stopAll();play("scare",s.scare==6?"XSCREAM2":"XSCREAM",false,.8f);break;
                case "@dead":stopAll();play("static","static",true,.3f);break;
                case "@win":stopAll();play("win","chimes 2",false,.8f);break;
                default:
                    String channel=event.startsWith("Laugh")?"freddy":event.startsWith("OVEN")?"kitchen":event.equals("music box")?"music":event;
                    float vol=event.equals("deep steps")?.2f:event.equals("circus")?.05f:event.equals("pirate song2")?s.pirateVolume():event.startsWith("OVEN")?s.kitchenNoiseVolume():.6f;
                    play(channel,event,false,vol);break;
            }
        }
        volume("light",s.leftLight||s.rightLight?.75f:0);volume("phone",s.mutedCall?0:s.monitor?.4f:.8f);
        volume("fan",s.monitor?.1f:.25f);volume("kitchen",s.kitchenNoiseVolume());
        volume("pirate song2",s.pirateVolume());volume("kitchenMusic",s.kitchenMusicVolume());
    }
}

