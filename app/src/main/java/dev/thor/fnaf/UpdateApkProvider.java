package dev.thor.fnaf;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Exposes exactly one private update APK, read-only, to Android's installer by URI grant. */
public final class UpdateApkProvider extends ContentProvider {
    static final String MIME="application/vnd.android.package-archive";
    static File apk(Context context){return new File(context.getCacheDir(),"updates/update.apk");}
    static Uri uri(Context context){return Uri.parse("content://"+context.getPackageName()+".updates/update.apk");}
    private File resolve(Uri uri)throws FileNotFoundException{
        if(!uri.equals(uri(getContext())))throw new FileNotFoundException("Unknown update file.");
        return apk(getContext());
    }
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){return MIME;}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        if(!"r".equals(mode))throw new FileNotFoundException("Read-only update.");
        return ParcelFileDescriptor.open(resolve(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){
        try{
            File apk=resolve(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
            MatrixCursor cursor=new MatrixCursor(columns);Object[] row=new Object[columns.length];
            for(int i=0;i<columns.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?"FNaF Redone update.apk":OpenableColumns.SIZE.equals(columns[i])?apk.length():null;
            cursor.addRow(row);return cursor;
        }catch(FileNotFoundException e){return null;}
    }
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
