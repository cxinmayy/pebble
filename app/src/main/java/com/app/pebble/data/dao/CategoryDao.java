package com.app.pebble.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.app.pebble.data.model.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    long insert(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories ORDER BY name ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<Category> getAllCategoriesSync();

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    LiveData<List<Category>> getCategoriesByType(String type);

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    List<Category> getCategoriesByTypeSync(String type);

    @Query("SELECT * FROM categories WHERE id = :id")
    Category getCategoryByIdSync(int id);

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    LiveData<Integer> getTransactionCountForCategory(int categoryId);

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    int getTransactionCountForCategorySync(int categoryId);
}
