package dev.thor.fnaf;

import java.nio.file.*;
import java.util.*;

/** Synthetic Steam layouts only; no retail assets required. */
public final class SteamLocatorTest {
    static int checks;
    static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static void write(Path file,String text)throws Exception{Files.createDirectories(file.getParent());Files.writeString(file,text);}
    static String quoted(Path p){return "\""+p.toAbsolutePath().toString().replace("\\","\\\\")+"\"";}
    static Path game(Path library,String folder)throws Exception{
        Path exe=library.resolve("steamapps/common").resolve(folder).resolve(SteamLocator.EXE);
        write(exe,"discovery fixture, never executable");return exe.toRealPath();
    }
    public static void main(String[] args)throws Exception{
        Path temp=Files.createTempDirectory("fnaf-steam-locator-");
        try{
            Path root=temp.resolve("Steam"),extra=temp.resolve("Other drive/library é"),legacy=temp.resolve("Legacy library");
            check(SteamLocator.findInRoots(List.of(root)).isEmpty(),"missing Steam returns no result");
            Path standard=game(root,"Five Nights at Freddy's");
            check(SteamLocator.findInRoots(List.of(root)).equals(List.of(standard)),"standard location works without manifest");
            Path renamed=game(extra,"Custom FNaF folder");
            write(extra.resolve("steamapps/appmanifest_319510.acf"),"\"AppState\" { \"appid\" \"319510\" \"installdir\" \"Custom FNaF folder\" }");
            write(root.resolve("steamapps/libraryfolders.vdf"),"\uFEFF\"libraryfolders\" { // modern library\n\"0\" { \"path\" "+quoted(root)+" } \"1\" { \"path\" "+quoted(extra)+" \"apps\" { \"319510\" \"1\" } } }");
            check(SteamLocator.findInRoots(List.of(root)).equals(List.of(standard,renamed)),"modern secondary library, escaped/Unicode path and manifest directory");
            check(SteamLocator.findInRoots(List.of(root,extra,root.resolve("."))).size()==2,"duplicate libraries produce unique real paths");
            Path old=game(legacy,"Five Nights at Freddy's");
            write(root.resolve("config/libraryfolders.vdf"),"\"LibraryFolders\" { \"1\" "+quoted(legacy)+" }");
            check(SteamLocator.findInRoots(List.of(root)).contains(old),"legacy config library format");
            Files.delete(renamed);
            check(!SteamLocator.findInRoots(List.of(root)).contains(renamed),"stale manifest/uninstalled game excluded");
            write(root.resolve("steamapps/libraryfolders.vdf"),"\"libraryfolders\" { \"1\" { \"path\" \"broken");
            check(SteamLocator.findInRoots(List.of(root)).equals(List.of(standard,old)),"malformed VDF does not prevent fallback or legacy libraries");
            write(extra.resolve("steamapps/appmanifest_319510.acf"),"\"AppState\" { \"appid\" \"319510\" \"installdir\" \"../../../outside\" }");
            write(extra.resolve("../outside").normalize().resolve(SteamLocator.EXE),"not a Steam game");
            check(SteamLocator.findInRoots(List.of(extra)).isEmpty(),"manifest cannot escape common directory");
            game(extra,"Wrong game");
            write(extra.resolve("steamapps/appmanifest_319510.acf"),"\"AppState\" { \"appid\" \"332800\" \"installdir\" \"Wrong game\" }");
            check(SteamLocator.findInRoots(List.of(extra)).isEmpty(),"different app ID is not imported");
            Files.write(standard,new byte[0]);
            check(!SteamLocator.findInRoots(List.of(root)).contains(standard),"empty/incomplete file excluded");
            check(SteamLocator.findInRoots(List.of(temp.resolve("missing"),legacy)).equals(List.of(old)),"one unavailable library does not hide a working one");
            System.out.println("Steam locator: "+checks+" checks passed.");
        }finally{
            // Delete only the fixture tree returned by createTempDirectory.
            try(var files=Files.walk(temp)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
        if(args.length>0&&args[0].equals("--installed")){
            List<Path> installed=SteamLocator.findInstalled();
            if(installed.isEmpty())throw new AssertionError("No real Steam installation discovered");
            for(Path p:installed)System.out.println("Found: "+p);
        }
    }
}
