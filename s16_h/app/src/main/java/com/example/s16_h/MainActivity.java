package com.example.s16_h;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private TextView txtNum, modifyCargoIndex; // 宣告空資料，資料型別為TextView
    private DbOpenHelper mUserDbOpenHelper, mCargoDbOpenHelper; // 宣告空資料，資料型別為自訂的繼承class
    private EditText edtItem, edtPrice, modifyCargoName, modifyCargoPrice; // 宣告空資料，資料型別為EditText
    private Button btnAdd, btnQuery, btnList, btnClear, btnDelete, btnModify, btnCancel; // 宣告空資料，資料型別為Button
    private ListView cargoList; // 宣告空資料，資料型別為ListView
    private Dialog modify_layout;
    private AlertDialog algBuilder, algBuilderClearDB;
    private int currentCargoIndex;

    //UserDb 專門用來儲存使用者資料，但在該專案中僅用來記錄該使用者最後一筆資料的index數，刻意不使用原先的primary自增值
    private static final String UserDB_FILE = "User.db", UserDB_TABLE = "User"; // 宣告字串，字串文字不可更改
    //CargoDb 用來記錄每一筆貨物資料
    private static final String CargoDB_FILE = "Cargo.db", CargoDB_TABLE = "Cargo"; // 宣告字串，字串文字不可更改

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 設定顯示layout為activity_main

        txtNum = findViewById(R.id.txtNum);

        edtItem = findViewById(R.id.edtItem); // 將空資料指向activity的物件
        edtPrice = findViewById(R.id.edtPrice); // 將空資料指向activity的物件

        btnAdd = findViewById(R.id.btnAdd); // 將空資料指向activity的物件
        btnQuery = findViewById(R.id.btnQuery); // 將空資料指向activity的物件
        btnList = findViewById(R.id.btnList); // 將空資料指向activity的物件
        btnClear = findViewById(R.id.btnClear); // 將空資料指向activity的物件

        cargoList = findViewById(R.id.cargoList); // 將空資料指向activity的物件
        cargoList.setOnItemClickListener(txtListOnItemClick);

        btnAdd.setOnClickListener(OnClickAdd); // 按鈕設定listen事件
        btnQuery.setOnClickListener(OnClickQuery); // 按鈕設定listen事件
        btnList.setOnClickListener(OnClickList); // 按鈕設定listen事件
        btnClear.setOnClickListener(OnClickClear); // 按鈕設定listen事件

        modify_layout = new Dialog(MainActivity.this);
        modify_layout.setContentView(R.layout.modify_layout);

        modifyCargoIndex = modify_layout.findViewById(R.id.modifyCargoIndex);
        modifyCargoName = modify_layout.findViewById(R.id.modifyCargoName);
        modifyCargoPrice = modify_layout.findViewById(R.id.modifyCargoPrice);

        btnDelete = modify_layout.findViewById(R.id.btnDelete);
        btnModify = modify_layout.findViewById(R.id.btnModify);
        btnCancel = modify_layout.findViewById(R.id.btnCancel);

        btnDelete.setOnClickListener(modifyBtnListen);
        btnModify.setOnClickListener(modifyBtnListen);
        btnCancel.setOnClickListener(modifyBtnListen);

        CheckDbOrInit();
        RenewList();
    }

    //清除資料庫按鍵傾聽
    private View.OnClickListener OnClickClear = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            algBuilderClearDB = new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.btnClear)
                    .setMessage(R.string.alert_Clear_message)
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.btnClearFinal), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ClearDbTable();
                            CheckDbOrInit();
                            RenewList();
                            Toast.makeText(MainActivity.this, getString(R.string.alert_delete_toast), Toast.LENGTH_SHORT).show();
                            algBuilderClearDB.dismiss();
                        }
                    })
                    .setNegativeButton(getString(R.string.btnCancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            algBuilderClearDB.dismiss();
                        }
                    })
                    .show();
        }
    };

    //新增貨物按鍵傾聽
    private View.OnClickListener OnClickAdd = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            SQLiteDatabase UserDb = mUserDbOpenHelper.getWritableDatabase(); // 從 DbOpenHelper 創建或打開可以操作的資料庫物件，這邊都會是打開。
            SQLiteDatabase CargoDb = mCargoDbOpenHelper.getWritableDatabase(); // mCargoDbOpenHelper 創建或打開可以操作的資料庫，這邊都會是打開。

            if (edtItem.getText().toString().equals("") || edtPrice.getText().toString().equals("")) {//檢查有沒有空的資料欄位
                Toast.makeText(MainActivity.this, getString(R.string.msg1), Toast.LENGTH_SHORT).show();//若有空的資料欄位，提醒使用者要填好
            } else {
                //設定下一個貨物編號
                Cursor userDb_Cursor = UserDb
                        .rawQuery("select * from " + UserDB_TABLE, null); // 建立光標資料，指向Db的查詢集合結果
                userDb_Cursor.moveToFirst();
                int index = userDb_Cursor.getColumnIndexOrThrow("nextCargoIndex");
                String nextCargoIndex = userDb_Cursor.getString(index);
                txtNum.setText(nextCargoIndex);

                Log.v("test", nextCargoIndex);
                ContentValues newRow = new ContentValues();//製作存入資料庫的ContentValues物件

                newRow.put("cargoIndex", nextCargoIndex);//ContentValues物件加入資料
                newRow.put("item", edtItem.getText().toString());
                newRow.put("price", edtPrice.getText().toString());

                CargoDb.insert(CargoDB_TABLE, null, newRow); //把ContentValues資料注入db

                //把貨物編號+1，產生下一件貨物的貨物編號。
                int nextCargoIndexInt = Integer.parseInt(nextCargoIndex);
                nextCargoIndexInt++;
                String str = "UPDATE " + UserDB_TABLE + " SET nextCargoIndex=" + nextCargoIndexInt + " WHERE _id=1";
                Log.v("test", String.valueOf(nextCargoIndexInt));

                UserDb.execSQL(str); // 建立光標資料，指向Db的查詢集合結果

                txtNum.setText(String.valueOf(nextCargoIndexInt));
                edtItem.setText("");//清空輸入欄位
                edtPrice.setText("");//清空輸入欄位

                Toast.makeText(MainActivity.this, getString(R.string.msg2), Toast.LENGTH_SHORT).show();//顯示新增資料完成訊息
            }
            CargoDb.close();// 關閉資料庫讀寫
            RenewList();
        }
    };

    //查詢貨物按鍵傾聽
    private View.OnClickListener OnClickQuery = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            SQLiteDatabase CargoDb = mCargoDbOpenHelper.getWritableDatabase(); // mCargoDbOpenHelper 創建或打開可以操作的資料庫，這邊是打開

            Cursor cursor = null;//清空光標

            if (edtItem.getText().toString().equals("") && edtPrice.getText().toString().equals("")) {
                Toast.makeText(MainActivity.this, getString(R.string.msg3), Toast.LENGTH_SHORT).show();//如果兩個查找欄位都是空的，提醒使用者填入任一欄位以便搜尋。
            } else if (!edtItem.getText().toString().equals("")) {//若品名不為空，優先搜尋品名。
                cursor = CargoDb.query(true, CargoDB_TABLE, new String[]{"_id", "cargoIndex", "item", "price"},
                        "item=" + "\"" + edtItem.getText().toString() + "\"", null, null, null, null, null);
            } else if (!edtPrice.getText().toString().equals("")) {//若品名為空，價格不為空，則搜尋價格。
                cursor = CargoDb.query(true, CargoDB_TABLE, new String[]{"_id", "cargoIndex", "item", "price"},
                        "price=" + "\"" + edtPrice.getText().toString() + "\"", null, null, null, null, null);
            }

            if (cursor == null) {//若光標為空，直接return
                return;
            }

            //顯示資料到list上
            cursor.moveToFirst();//光標移到開頭
            UpdateAdapter(cursor);//更新顯示

            if (cursor.getCount() == 0) {//查光標回傳的資料錄數量是不是0
                if (!edtItem.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, getString(R.string.msg_no_item), Toast.LENGTH_SHORT).show();//告知使用者無此品名。
                }
                if (!edtPrice.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, getString(R.string.msg_no_price), Toast.LENGTH_SHORT).show();//告知使用者無此價格。
                }
                return;
            }

            Toast.makeText(MainActivity.this, getString(R.string.msg_data_found), Toast.LENGTH_SHORT).show();//顯示查詢完畢
            CargoDb.close();// 關閉資料庫讀寫
        }
    };

    //查詢所有貨物按鍵傾聽
    private View.OnClickListener OnClickList = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            SQLiteDatabase CargoDb = mCargoDbOpenHelper.getWritableDatabase(); // mCargoDbOpenHelper 創建或打開可以操作的資料庫，這邊是打開

            Cursor cursor = CargoDb.query(true, CargoDB_TABLE, new String[]{"_id", "cargoIndex", "item", "price"}, null, null, null,
                    null, null, null);//光標查找所有

            if (cursor == null) //若光標沒有內容則return
                return;

            if (cursor.getCount() == 0) {//有顯示資料，但資料內容為0
                Toast.makeText(MainActivity.this, getString(R.string.no_data), Toast.LENGTH_SHORT).show();//警告使用者沒有資料
            }

            cursor.moveToFirst();//光標移到開頭
            UpdateAdapter(cursor);//更新顯示

            CargoDb.close();// 關閉資料庫讀寫
        }
    };

    //清單、匿名修改頁面傾聽
    private AdapterView.OnItemClickListener txtListOnItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long l) {

            SQLiteDatabase CargoDb = mCargoDbOpenHelper.getWritableDatabase();
            Cursor cursor = CargoDb.query(true, CargoDB_TABLE, new String[]{"_id", "cargoIndex", "item", "price"},
                    "cargoIndex=" + "\"" + l + "\"", null, null, null, null, null);

            cursor.moveToFirst();
            try {
                currentCargoIndex = ((int) l);
                modifyCargoIndex.setText(cursor.getString(1));
                modifyCargoName.setText(cursor.getString(2));
                modifyCargoPrice.setText(cursor.getString(3));
            } catch (Exception e) {
            }

            modify_layout.show();
            cursor.close();
            CargoDb.close();
        }
    };

    //貨物變更單，按鈕傾聽
    private View.OnClickListener modifyBtnListen = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            SQLiteDatabase CargoDb = mCargoDbOpenHelper.getWritableDatabase();

            switch (view.getId()) {
                case R.id.btnDelete:
                    algBuilder = new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.alert_delete_title)
                            .setMessage(R.string.alert_delete_message)
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.btnDelete), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    CargoDb.execSQL("DELETE FROM " + CargoDB_TABLE + " WHERE cargoIndex=" + currentCargoIndex);
                                    Toast.makeText(MainActivity.this, getString(R.string.alert_delete_toast), Toast.LENGTH_SHORT).show();
                                    CargoDb.close();
                                    RenewList();
                                    algBuilder.dismiss();
                                    modify_layout.dismiss();
                                }
                            })
                            .setNegativeButton(getString(R.string.btnCancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    CargoDb.close();
                                    RenewList();
                                    algBuilder.dismiss();
                                    modify_layout.dismiss();
                                }
                            })
                            .show();
                    break;
                case R.id.btnModify:
                    algBuilder = new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.alert_modify_title)
                            .setMessage(R.string.alert_modify_message)
                            .setPositiveButton(getString(R.string.btnModify), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String str = "UPDATE " + CargoDB_TABLE + " SET item='" + modifyCargoName.getText() + "', price='" + modifyCargoPrice.getText() + "' WHERE cargoIndex=" + currentCargoIndex;
                                    CargoDb.execSQL(str);
                                    Toast.makeText(MainActivity.this, getString(R.string.alert_modify_toast), Toast.LENGTH_SHORT).show();
                                    CargoDb.close();
                                    RenewList();
                                    algBuilder.dismiss();
                                    modify_layout.dismiss();
                                }
                            })
                            .setNegativeButton(getString(R.string.btnCancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    CargoDb.close();
                                    RenewList();
                                    algBuilder.dismiss();
                                    modify_layout.dismiss();
                                }
                            })
                            .show();
                    break;
                case R.id.btnCancel:
                    CargoDb.close();
                    RenewList();
                    modify_layout.dismiss();
                    break;
            }
        }
    };

    public void UpdateAdapter(Cursor cursor) {
        if (cursor != null && cursor.getCount() >= 0) {
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                    R.layout.cargo_layout, cursor, new String[]{"_id", "item", "price"},
                    new int[]{R.id.txtId, R.id.txtName, R.id.txtPrice}, 0);
            cargoList.setAdapter(adapter);
        }
    }

    //打開或創建UserDb、CargoDb資料庫，並檢查有無資料表，若無有缺少則 1.創建資料庫 2.創建基礎資料表、3.創建預設資料錄；   注意此功能不能修復損毀的資料表。
    private void CheckDbOrInit() {

        mUserDbOpenHelper = new DbOpenHelper(getApplicationContext(), UserDB_FILE, null, 1); // 實體化DbOpenHelper物件，資料庫名稱為UserDB_FILE，版本1
        mCargoDbOpenHelper = new DbOpenHelper(getApplicationContext(), CargoDB_FILE, null, 1); // 實體化DbOpenHelper物件，資料庫名稱為CargoDB_FILE，版本1

        SQLiteDatabase UserDb = mUserDbOpenHelper.getWritableDatabase(); // 從 DbOpenHelper 創建或打開可以操作的資料庫物件，這邊是創建。
        SQLiteDatabase CargoDb = mCargoDbOpenHelper.getWritableDatabase(); // 從 mCargoDbOpenHelper 創建或打開可以操作的資料庫物件，這邊是創建。


        //光標查詢
        Cursor userDb_Cursor = UserDb
                .rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_Name = '" + UserDB_FILE + "'", null); // 建立光標資料，指向Db的查詢集合結果
        Cursor cargoDb_Cursor = CargoDb
                .rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_Name = '" + CargoDB_TABLE + "'", null); // 建立光標資料，指向Db的查詢集合結果

        //檢查並建立基礎資料表、資料錄
        if (userDb_Cursor != null) {
            try {
                if (userDb_Cursor.getCount() == 0) {
                    UserDb.execSQL("CREATE TABLE " + UserDB_TABLE + "(_id INTEGER NOT NULL PRIMARY KEY,nextCargoIndex INTEGER NOT NULL)");// 創建資料表

                    ContentValues newRow = new ContentValues();//製作存入資料庫的ContentValues物件。
                    newRow.put("nextCargoIndex", "1");//在User新增一筆資料錄用來記錄下一組貨物的貨物編號。
                    UserDb.insert(UserDB_TABLE, null, newRow);
                }
            } catch (Exception e) {
            }
        }

        if (cargoDb_Cursor != null) {// 檢查光標集合是否為空，為空的話代表沒有資料表。
            if (cargoDb_Cursor.getCount() == 0) {
                CargoDb.execSQL("CREATE TABLE " + CargoDB_TABLE + "(_id INTEGER NOT NULL PRIMARY KEY,cargoIndex INTEGER NOT NULL, item TEXT NOT NULL, price TEXT NOT NULL)");// 創建資料表
//                CargoDb.execSQL("INSERT INTO " + CargoDB_TABLE + "(cargoIndex,item,price) VALUES (1,\"Potato\",'234')");//測試資料
//                CargoDb.execSQL("INSERT INTO " + CargoDB_TABLE + "(cargoIndex,item,price) VALUES (2,\"Tomato\",'2234')");
//                CargoDb.execSQL("INSERT INTO " + CargoDB_TABLE + "(cargoIndex,item,price) VALUES (3,\"Mango\",'1754')");
//                CargoDb.execSQL("INSERT INTO " + CargoDB_TABLE + "(cargoIndex,item,price) VALUES (4,\"Peach\",'972')");
//                CargoDb.execSQL("INSERT INTO " + CargoDB_TABLE + "(cargoIndex,item,price) VALUES (5,\"Vegetable\",'123')");
//                CargoDb.execSQL("INSERT INTO " + CargoDB_TABLE + "(cargoIndex,item,price) VALUES (6,\"DragonFruit\",'9992')");

            }
        }

        //顯示下一個貨物編號
        userDb_Cursor = UserDb
                .rawQuery("select * from " + UserDB_TABLE, null); // 建立光標資料，指向Db的查詢集合結果

        userDb_Cursor.moveToFirst();
        int index = userDb_Cursor.getColumnIndexOrThrow("nextCargoIndex");
        String nextCargoIndex = userDb_Cursor.getString(index);
        txtNum.setText(nextCargoIndex);

        userDb_Cursor.close();// 結束光標
        cargoDb_Cursor.close();// 結束光標
        UserDb.close();// 關閉資料庫讀寫
        CargoDb.close();// 關閉資料庫讀寫
    }

    //刪除UserDb、CargoDb
    private void ClearDbTable() {
        mUserDbOpenHelper = new DbOpenHelper(getApplicationContext(), UserDB_FILE, null, 1); // 實體化DbOpenHelper物件，資料庫名稱為UserDB_FILE，版本1
        mCargoDbOpenHelper = new DbOpenHelper(getApplicationContext(), CargoDB_FILE, null, 1); // 實體化DbOpenHelper物件，資料庫名稱為CargoDB_FILE，版本1

        SQLiteDatabase UserDb = mUserDbOpenHelper.getWritableDatabase(); // 從 DbOpenHelper 創建或打開可以操作的資料庫物件，這邊是創建。
        SQLiteDatabase CargoDb = mCargoDbOpenHelper.getWritableDatabase(); // 從 mCargoDbOpenHelper 創建或打開可以操作的資料庫物件，這邊是創建。

//        UserDb.execSQL("DROP TABLE IF EXISTS " + UserDB_TABLE);
//        CargoDb.execSQL("DROP TABLE IF EXISTS " + CargoDB_TABLE);

        this.deleteDatabase(UserDB_FILE);
        this.deleteDatabase(CargoDB_FILE);

//        UserDb.close();// 關閉資料庫讀寫
//        CargoDb.close();// 關閉資料庫讀寫
    }

    //刷新清單
    private void RenewList() {
        mCargoDbOpenHelper = new DbOpenHelper(getApplicationContext(), CargoDB_FILE, null, 1); // 實體化DbOpenHelper物件，資料庫名稱為CargoDB_FILE，版本1

        SQLiteDatabase CargoDb = mCargoDbOpenHelper.getWritableDatabase(); // 從 mCargoDbOpenHelper 創建或打開可以操作的資料庫物件，這邊是創建。

        Cursor cursor = CargoDb.rawQuery("SELECT * FROM " + CargoDB_TABLE, null);
        UpdateAdapter(cursor);
    }
}