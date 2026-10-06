"""Build shared rendering/gameplay sources with a desktop host. No game assets needed."""
from pathlib import Path
root=Path(__file__).resolve().parent
source=root.parent/'app/src/main/java/dev/thor/fnaf'
out=root/'build/generated/dev/thor/fnaf';out.mkdir(parents=True,exist_ok=True)
version=(root/'VERSION').read_text().strip()
for n in ['PanelView','GameAssets','GameAudio','GameState','Progress','NightIntro','NightOutro','TitleEffects','NightSkipChord','ScreenMode','SteamImporter','ReleaseConfig']:
 text=(source/(n+'.java')).read_text(encoding='utf-8-sig')
 if n=='ReleaseConfig':
  text=text.replace('static final boolean', f'static final String VERSION="{version}-windows-preview";\n    static final boolean')
 if n=='PanelView':
  text=text.replace("1. In Steam, browse FNaF 1's installed files.\\n2. Copy FiveNightsatFreddys.exe to this device.\\n3. Select it below. Setup happens here, offline.\\n\\nAn existing FNaF 1 data ZIP also works.\\nNo Steam login or PC extraction tool is needed.", "Install FNaF 1 in Steam, then scan your libraries.\\nA detected copy is imported automatically.\\n\\nYou can also browse to your Steam EXE or data ZIP.\\nSetup works offline. No Steam login is needed.")
  text=text.replace('button("A  SELECT STEAM FILE",45,530,540,55,640,true);', 'button("A / ENTER  SCAN STEAM",45,470,540,55,640,true);\\n                button("X / SPACE  BROWSE FILE",45,535,540,55,641,false);\\n                text(app.steamScanStatus,45,420,16,MUTED);'.replace('\\n','\n'))
  text=text.replace('else if(id==640)app.chooseLocalFile(10);','else if(id==640)app.chooseLocalFile(10);\n        else if(id==641)app.browseLocalFile(10);')
  text=text.replace('if(app.assets.importing)return;', 'if(app.assets.importing||app.scanningSteam)return;')
  text=text.replace('if(app.assets.importing){rect', 'if(app.scanningSteam){rect(0,105,1000,545,0xf0000000);text(app.steamScanStatus,45,340,24,WHITE);}\n            if(app.assets.importing){rect')
  text=text.replace('Touch controls work on both screens.', 'Mouse/touch: click controls. F11: fullscreen.')
  text=text.replace('Native dual-screen build.', 'Windows single-/dual-screen build.')
  text=text.replace('c.save();c.scale(getWidth()/1000f,getHeight()/650f);', 'c.save();float[] viewport=DisplayLayout.fit(getWidth(),getHeight());c.translate(viewport[0],viewport[1]);c.scale(viewport[2]/1000f,viewport[3]/650f);')
  text=text.replace('if(!app.assets.ready)text("Select New Game', 'if(app.options){text("FNaF Redone "+ReleaseConfig.VERSION,45,610,17,MUTED);text(app.detectedDisplay,45,636,16,MUTED);}\n            if(!app.assets.ready&&!app.options)text("Select New Game')
 (out/(n+'.java')).write_text(text,encoding='utf-8')
s=(source/'MainActivity.java').read_text(encoding='utf-8-sig')
fields=s[s.index('    static final double MENU_REVEAL_DURATION'):s.index('    @Override public void onCreate')]
fields=fields.replace('    DisplayManager displays;','').replace('Presentation presentation;', 'DesktopPresentation presentation;')
frame=s[s.index('    final Runnable frame ='):s.index('    void dismissDisplay()')]
frame=frame.replace('primary.invalidate()', 'primary.repaint()').replace('secondary.invalidate()', 'secondary.repaint()')
frame=frame.replace('handler.postDelayed(this,16);','')
frame=frame.replace('if (!resumed) return;', '''if (!resumed) return;
        if(!focused()) { timing.active(false);lastFrame=SystemClock.elapsedRealtimeNanos();audio.pause(true);return; }
        timing.active(true);if(controller!=null)controller.poll();audio.pause(state.paused);''')
logic=s[s.index('    void releaseInputs()'):s.index('    @Override public void onBackPressed')]
logic=logic.replace('"Import Steam file"','"Scan / import Steam copy"')
logic=logic.replace('"Import local font","Back"', '"Import local font",checkingUpdates?"Checking GitHub...":"Check for updates","Back"')
logic=logic.replace('new int[]{17,18,10,2,3,4,5,6,7,16,11}', 'new int[]{17,18,10,2,3,4,5,6,7,16,19,11}')
logic=logic.replace('case 18:', 'case 19:checkForUpdates();break;\n            case 18:')
logic=logic.replace('void releaseInputs() {', 'void releaseInputs() { desktopLeft=desktopRight=false;')
inputs=s[s.index('    boolean handleKey('):s.index('    String diagnostics()')]
inputs=inputs.replace('if(assets.importing)return true;','if(assets.importing||scanningSteam)return true;')
inputs=inputs.replace('state.axis=down?(k==KeyEvent.KEYCODE_A?-1:1):0;', 'if(k==KeyEvent.KEYCODE_A)desktopLeft=down;else desktopRight=down;state.axis=desktopLookAxis(controller==null?0:controller.axisX);')
inputs=inputs.replace('state.axis=x;', 'state.axis=desktopLookAxis(x);')
inputs=inputs.replace('if(k==KeyEvent.KEYCODE_BUTTON_B)back();\n                else if(showHelp', 'if(k==KeyEvent.KEYCODE_BUTTON_B)back();\n                else if(showHelp&&!assets.ready&&k==KeyEvent.KEYCODE_BUTTON_X)browseLocalFile(10);\n                else if(showHelp')
ending=s[s.index('    void completeOutro()'):s.rfind('}')]
template=(root/'src/dev/thor/fnaf/MainActivity.template').read_text(encoding='utf-8')
(out/'MainActivity.java').write_text(template.replace('/* SHARED_FIELDS */',fields).replace('/* SHARED_FRAME */',frame).replace('/* SHARED_LOGIC */',logic).replace('/* SHARED_INPUT */',inputs).replace('/* SHARED_ENDING */',ending),encoding='utf-8')
