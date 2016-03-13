package com.fhacktory.sketchracer;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.AndroidFragmentApplication;

public class AndroidLauncher extends FragmentActivity implements AndroidFragmentApplication.Callbacks {
	private Circuit circuit;
    private int turns;

    @Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		circuit = getIntent().getParcelableExtra("circuit");
		turns = getIntent().getIntExtra("turns", 1);
		// 6. Finally, replace the AndroidLauncher activity content with the Libgdx Fragment.
		GameFragment fragment = new GameFragment();
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(android.R.id.content, fragment);
		trans.commit();
	}

	// 4. Create a Class that extends AndroidFragmentApplication which is the Fragment implementation for Libgdx.
	private class GameFragment extends AndroidFragmentApplication
	{
		// 5. Add the initializeForView() code in the Fragment's onCreateView method.
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			return initializeForView(new SketchRacer(circuit, turns));
		}
	}


	@Override
	public void exit() {}

}
