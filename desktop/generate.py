"""Build shared rendering/gameplay sources with a desktop host. No game assets needed."""
from pathlib import Path
root=Path(__file__).resolve().parent
source=root.parent/'app/src/main/java/dev/thor/fnaf'
out=root/'build/generated/dev/thor/fnaf';out.mkdir(parents=True,exist_ok=True)
for n in ['PanelView','GameAssets','GameAudio','GameState','Progress','NightIntro','NightOutro','TitleEffects','NightSkipChord','ScreenMode','SteamImporter','ReleaseConfig']:
 text=(source/(n+'.java')).read_text(encoding='utf-8-sig')
 if n=='PanelView':
  text=text.replace('2. Copy FiveNightsatFreddys.exe to this device.', '2. Find FiveNightsatFreddys.exe in that folder.')
  text=text.replace('Touch controls work on both screens.', 'Mouse/touch: click controls. F11: fullscreen.')
  text=text.replace('Native dual-screen build.', 'Windows single-/dual-screen build.')
 (out/(n+'.java')).write_text(text,encoding='utf-8')
s=(source/'MainActivity.java').read_text(encoding='utf-8-sig')
fields=s[s.index('    static final double MENU_REVEAL_DURATION'):s.index('    @Override public void onCreate')]
fields=fields.replace('    DisplayManager displays;','').replace('Presentation presentation;', 'DesktopPresentation presentation;')
frame=s[s.index('    final Runnable frame ='):s.index('    void dismissDisplay()')]
frame=frame.replace('if (!resumed) return;', '''if (!resumed) return;
        if(!focused()) { lastFrame=SystemClock.elapsedRealtimeNanos();audio.pause(true);primary.invalidate();if(secondary!=null)secondary.invalidate();handler.postDelayed(this,50);return; }
        audio.pause(state.paused);''')
logic=s[s.index('    void releaseInputs()'):s.index('    @Override public void onBackPressed')]
inputs=s[s.index('    boolean handleKey('):s.index('    String diagnostics()')]
ending=s[s.index('    void completeOutro()'):s.rfind('}')]
template=(root/'src/dev/thor/fnaf/MainActivity.template').read_text(encoding='utf-8')
(out/'MainActivity.java').write_text(template.replace('/* SHARED_FIELDS */',fields).replace('/* SHARED_FRAME */',frame).replace('/* SHARED_LOGIC */',logic).replace('/* SHARED_INPUT */',inputs).replace('/* SHARED_ENDING */',ending),encoding='utf-8')
