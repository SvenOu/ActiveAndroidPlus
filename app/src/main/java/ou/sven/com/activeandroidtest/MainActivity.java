package ou.sven.com.activeandroidtest;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.IntegerRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Configuration;

import ou.sven.com.activeandroidtest.db.Category;
import ou.sven.com.activeandroidtest.db.Item;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //当重新安装app的时候才会自动执行migrations和创建model对应的表
        Configuration dbConfiguration = new Configuration.Builder(this.getApplication()).
                setDatabaseName("test_dbchg.db").
                setFormatType(Configuration.Builder.SQL_SCRIPT_XML_FORMAT).
                setDatabaseVersion(28).
                setModelClasses(Category.class,Item.class).
                create();
        new InitDbTask().execute(dbConfiguration);
    }

    private void testMyContentProvider() {
        Cursor cursor = getContentResolver().query(Uri.parse("content://ou.sven.com.activeandroidtest/users"),
                new String[]{"name"},
                null, null, null);
        while (cursor.moveToNext()) {
            Log.i(TAG, cursor.getString(cursor.getColumnIndex("name")));
        }
        startActivity(new Intent("android.intent.action.VIEW", Uri.parse("content://ou.sven.com.activeandroidtest/users")));
    }

    private class InitDbTask extends AsyncTask<Configuration, Void, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected Void doInBackground(Configuration... configurations) {
            Configuration conf = configurations[0];
            ActiveAndroid.initialize(conf);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.e(TAG, "-----------数据库初始化完毕------------");
            testMyContentProvider();
        }
    }
}
