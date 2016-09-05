package ou.sven.com.activeandroidtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.activeandroid.Model;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.Log;

import java.util.Date;
import java.util.List;

import ou.sven.com.activeandroidtest.db.Category;
import ou.sven.com.activeandroidtest.db.Item;

public class Main2Activity extends AppCompatActivity {
    private static final String TAG = Main2Activity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        dbTest();
    }

    private void dbTest(){
        // Create a category
        Category restaurants = Model.load(Category.class, 1);
        if(null == restaurants){
            restaurants = new Category();
        }
        restaurants.setCategoryId(1);
        restaurants.setName("Restaurants222");
        restaurants.setCreateDate(new Date());
        restaurants.save();


// Create an item
        Item item = Model.load(Item.class, 1);
        if(null == item){
            item =  new Item();
        }
        item.setItemId(1);
        item.setCategory(restaurants) ;
        item.setName( "Outback Steakhouse1111");
        item.save();

//// Deleting items
//        Item item1 = Item.load(Item.class, 1);
//        item1.delete();
//// or with
//        new Delete().from(Item.class).where("remote_id = ?", 1).execute();

        List<Item> items = new Select()
                .from(Item.class)
                .where("Category = ?", restaurants.getId())
                .orderBy("Name ASC")
                .execute();
        Log.e(TAG, item.toString());
    }
}
