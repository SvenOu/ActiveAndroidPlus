package ou.sven.com.activeandroidtest.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.Date;

@Table(name = "Items")
public class Item extends Model {
    // This is the unique id given by the server
    @Column(name = "item_id", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    private long itemId;
    // This is a regular field
    @Column(name = "Name")
    private String name;
    // This is an association to another activeandroid model
    @Column(name = "Category", onUpdate = Column.ForeignKeyAction.CASCADE, onDelete = Column.ForeignKeyAction.CASCADE)
    private Category category;

    @Column(name = "create_date")
    private Date createDate;

    // Make sure to have a default constructor for every ActiveAndroid model
    public Item(){
        super();
    }

    public Item(int itemId, String name, Category category){
        super();
        this.itemId = itemId;
        this.name = name;
        this.category = category;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}
