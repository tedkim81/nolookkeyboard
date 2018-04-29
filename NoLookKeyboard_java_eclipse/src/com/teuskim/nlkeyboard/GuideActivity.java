package com.teuskim.nlkeyboard;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class GuideActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.guide);
		NLPreference pref = new NLPreference(getApplicationContext());
		pref.setDidReadGuide(true);
		
		findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

}
