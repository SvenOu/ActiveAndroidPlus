package ou.sven.com.activeandroidtest;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by sven-ou on 2016/8/5.
 */
public class MyContentProvider extends ContentProvider{

    /**
     * 定义规则
     */
    public static final UriMatcher uriMatcher;
    public static final int USERS_COLLECTION = 1;//用于标记
    public static final int USERS_SINGLE = 2;//用于标记
    private static final String[] COLUMN_NAMES = {"name"};
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);//试用一个没有规则的Uri。然后下面自己匹配。
        uriMatcher.addURI("ou.sven.com.activeandroidtest","/users",USERS_COLLECTION);//自己定义的规则，有点像路由器，是uri匹配的方案。
        uriMatcher.addURI("ou.sven.com.activeandroidtest","/users/#",USERS_SINGLE);//同上。
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        MatrixCursor matrixCursor = new MatrixCursor(COLUMN_NAMES);
        matrixCursor.addRow(new Object[] {"fuck"});
        return matrixCursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.google.note";
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }
}
