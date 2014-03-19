package com.audiomanager.app;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.audiomanager.fragment.AudioRecorderFragment;
import com.audiomanager.obj.Item;


public class HomeActivity extends ActionBarActivity {
	private static Item[] sItems = {new Item(0), new Item(1), new Item(2)};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

	    final ListView listView = (ListView)findViewById(android.R.id.list);
	    listView.setAdapter(new ItemAdapter(this, sItems));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



	private class ItemAdapter extends ArrayAdapter<Item> {
		public ItemAdapter(Context context, Item[] objects) {
			super(context, R.layout.home_list_item, R.id.item_title, objects);
		}
	}

	public void onRecord(View v) {
		new AudioRecorderFragment().show(getSupportFragmentManager(), AudioRecorderFragment.TAG);
	}
}
