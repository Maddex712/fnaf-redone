package dev.thor.fnaf;

public final class DesktopSettingsTest {
    static int checks;
    static void check(boolean condition,String reason){checks++;if(!condition)throw new AssertionError(reason);}
    static String release(String tag,String asset,boolean draft){return "{\"tag_name\":\""+tag+"\",\"draft\":"+draft+",\"html_url\":\""+ReleaseUpdates.PAGE+"/tag/"+tag+"\",\"assets\":[{\"name\":\""+asset+"\"}]}";}
    public static void main(String[] args)throws Exception{
        for(int[] size:new int[][]{{1280,720},{1920,1200},{1900,1000},{2560,1600},{3440,1440},{720,1280}}){
            float[] fit=DisplayLayout.fit(size[0],size[1]);
            check(Math.abs(fit[2]/fit[3]-16f/9)<.0001,"content retains 16:9 on "+size[0]);
            check(fit[0]>=0&&fit[1]>=0&&fit[2]<=size[0]+.01&&fit[3]<=size[1]+.01,"content stays inside display");
            check(Math.abs(fit[0]*2+fit[2]-size[0])<.01&&Math.abs(fit[1]*2+fit[3]-size[1])<.01,"content centered");
        }
        check(DisplayLayout.describe(1920,1200).contains("16:10"),"16:10 detection");
        check(DisplayLayout.describe(1900,1000).contains("19:10"),"19:10 detection");
        check(DisplayLayout.describe(1920,1080).contains("16:9"),"16:9 detection");
        check(DisplayLayout.fit(0,0)[2]==0,"startup before layout is safe");
        String win="FNaF-Redone-0.2.3-Windows-x64-preview.zip";
        String list="["+release("v9.0.0-preview","FNaF-Redone-9.0.0.apk",false)+","+release("v0.2.3-windows-preview",win,false)+","+release("v0.2.1-windows-preview",win,false)+","+release("v8.0.0-windows-preview",win,true)+"]";
        ReleaseUpdates.Result result=ReleaseUpdates.select(list,"0.2.2-windows-preview","Windows");
        check(result.newer&&result.url.endsWith("v0.2.3-windows-preview"),"picks newest Windows asset, ignores Android and drafts");
        check(!ReleaseUpdates.select(list,"0.2.3-windows-preview","Windows").newer,"same version is current");
        check(!ReleaseUpdates.select(list,"0.3.0-windows-preview","Windows").newer,"never suggests a downgrade");
        check(ReleaseUpdates.compare("v0.10.0-preview","0.9.9-preview")>0,"numeric version ordering");
        check(ReleaseUpdates.compare("v0.2.3","0.2.3-windows-preview")>0,"stable supersedes same-number preview");
        check(!ReleaseUpdates.select("[]","0.2.2-windows-preview","Windows").newer,"empty release feed is safe");
        check(ReleaseUpdates.version("not-a-version")==null,"invalid tags ignored");
        check(ReleaseUpdates.version("v999999999999999.0.0")==null,"overflow tags ignored");
        String unsafe=list.replace(ReleaseUpdates.PAGE+"/tag/v0.2.3-windows-preview","https://example.com/installer");
        check(ReleaseUpdates.select(unsafe,"0.2.2-windows-preview","Windows").url.equals(ReleaseUpdates.PAGE),"download action stays on project GitHub releases");
        System.out.println("Display/update settings: "+checks+" checks passed");
        if(args.length>0&&args[0].equals("--live")){
            ReleaseUpdates.Result live=ReleaseUpdates.check(ReleaseConfig.VERSION,"Windows");
            if(!live.message.startsWith("You're up to date")&&!live.newer)throw new AssertionError(live.message);
            System.out.println("Live GitHub: "+live.message.replace('\n',' '));
        }
    }
}
