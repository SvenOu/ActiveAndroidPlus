package ou.sven.com.activeandroidtest.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.Date;
import java.util.List;

@Table(name = "Categories")
public class Category extends Model {
    // This is the unique id given by the server
    @Column(name = "category_id", unique = true)
    private long categoryId;
    // This is a regular field
    @Column(name = "name")
    private String name;

    @Column(name = "create_date")
    private Date createDate;

    // Make sure to have a default constructor for every ActiveAndroid model
    public Category(){
        super();
    }

    // Used to return items from another table based on the foreign key
    public List<Item> items() {
        return getMany(Item.class, "Category");
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}