package com.fhacktory.sketchracer;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.badlogic.gdx.backends.android.AndroidFragmentApplication;

public class AndroidLauncher extends AppCompatActivity implements AndroidFragmentApplication.Callbacks {
	private Circuit circuit;
    private int turns;
	private TextView hud1, hud2;

    @Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launcher);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		circuit = getIntent().getParcelableExtra("circuit");
		turns = getIntent().getIntExtra("turns", 1);
		hud1 = (TextView) findViewById(R.id.hud1);
		hud2 = (TextView) findViewById(R.id.hud2);

		// 6. Finally, replace the AndroidLauncher activity content with the Libgdx Fragment.
		GameFragment fragment = new GameFragment();
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(R.id.content, fragment);
		trans.commit();
	}

	// 4. Create a Class that extends AndroidFragmentApplication which is the Fragment implementation for Libgdx.
	private class GameFragment extends AndroidFragmentApplication
	{
		// 5. Add the initializeForView() code in the Fragment's onCreateView method.
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			return initializeForView(new SketchRacer(circuit, turns, AndroidLauncher.this));
		}
	}


	@Override
	public void exit() {}

	public void setHud1Blink(final String text) {
		new Thread() {
			@Override public void run() {
				try {
					//blink the text
					for (int i = 0; i < 5; i++) {
						setHud1(text);
						Thread.sleep(500);
						setHud1("");
						Thread.sleep(500);
					}
					setHud1(text);
				} catch(InterruptedException ie) {}
			}
		}.start();

	}

	public void setHud1(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				hud1.setText(text);
			}
		});
	}

	public void setHud2(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				hud2.setText(text);
			}
		});
	}

}
