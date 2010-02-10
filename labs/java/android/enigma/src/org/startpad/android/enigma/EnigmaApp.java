package org.startpad.android.enigma;

import org.startpad.Enigma;

import org.startpad.android.enigma.R;

import android.app.TabActivity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TabHost.OnTabChangeListener;

public class EnigmaApp extends TabActivity
	{
	private static final String TAG = "Enigma";
	
	boolean fHasMedia = false;
	enum SoundEffect { KEY_DOWN, KEY_UP, ROTOR };
    MediaPlayer mpDown;
    MediaPlayer mpUp;
    MediaPlayer mpRotor;
	
	EnigmaView sim;
	
	// Machine settings
    Spinner[] aspnRotors = new Spinner[3];
    Spinner[] aspnStart = new Spinner[3];
    Spinner[] aspnRings = new Spinner[3];
    EditText plugboard;
    
    int[] aRotors = new int[] {0,1,2};
    ToggleButton toggleGroup;
    boolean fGroup = true;
    EditText edit;
    TextView output;
    Enigma.Settings settings = new Enigma.Settings();
    Enigma machine = new Enigma(null);
    Toast toast;
    boolean fLegalSettings = true;
    String sSettingsError;
    TabHost tabHost;
    InputMethodManager imm;
    IBinder token;
    ClipboardManager cbm;
    
    private void updateEncoding()
        {
        String code;
        
        try
            {
            machine.init(null);
            code = machine.encode(edit.getText().toString());
            }
        catch (Exception e)
            {
            output.setText("- error -");
            return;
            }
        
        if (fGroup)
            code = Enigma.groupLetters(code);
        
        output.setText(code);
        
        // Always copy results to clipboard
        cbm.setText(code);
        }
    
    private void updateSettings()
        {
    	for (int i = 0; i < 3; i++)
    	    {
    	    String s = (String) aspnRotors[i].getSelectedItem();
    		settings.rotors[i] = s;
    	    }
    	
    	for (int i = 0; i < 3; i++)
    		settings.position[i] = ((String) aspnStart[i].getSelectedItem()).charAt(0);
    	
    	for (int i = 0; i < 3; i++)
    		settings.rings[i] = ((String) aspnRings[i].getSelectedItem()).charAt(0);
    	
    	settings.plugs = plugboard.getText().toString();
    	
    	fLegalSettings = true;
    	try
    	    {
    	    machine.init(settings);
    	    }
    	catch (Exception e)
    	    {
    	    fLegalSettings = false;
    	    sSettingsError = e.getMessage();
    	    Log.d(TAG, e.getMessage());
    	    }
    	
    	updateEncoding();
        }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    	{
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate");
        
        tabHost = getTabHost();
        imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
        cbm = (ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        toast = Toast.makeText(this, R.string.startup_message, Toast.LENGTH_LONG);
        toast.show();
        
        // Setup top-level tabbled layout screen 
        
        LayoutInflater.from(this).inflate(R.layout.main, tabHost.getTabContentView(), true);

        tabHost.addTab(tabHost.newTabSpec("sim")
                .setIndicator("Enigma")
                .setContent(R.id.sim));
        tabHost.addTab(tabHost.newTabSpec("encoder")
                .setIndicator("Message")
                .setContent(R.id.encoder));
        tabHost.addTab(tabHost.newTabSpec("settings")
                .setIndicator("Settings")
                .setContent(R.id.settings));
        tabHost.addTab(tabHost.newTabSpec("info")
                .setIndicator("Info")
                .setContent(R.id.enigma_info));
        

        // token = edit.getApplicationWindowToken();
        
        tabHost.setOnTabChangedListener(new OnTabChangeListener()
            {
            public void onTabChanged(String tabId)
                {
                // BUG: This is NOT working to hide the virtual keyboard
                // on all tab changes ... why not?
                // imm.hideSoftInputFromInputMethod(token, InputMethodManager.HIDE_NOT_ALWAYS);
                
                if (tabId.equals("sim") || tabId.equals("encoder"))
                    {
                    updateSettings();
                    if (!fLegalSettings)
                        {
                        tabHost.setCurrentTabByTag("settings");
                        toast.cancel();
                        toast.setText(sSettingsError);
                        toast.show();
                        }
                    }
                }
            });
        
        sim = (EnigmaView) findViewById(R.id.sim);
        sim.setMachine(machine);
        
        // Initialize Encoder view
        
        output = (TextView) findViewById(R.id.output);
        edit = (EditText) findViewById(R.id.input);
        
        toggleGroup = (ToggleButton) findViewById(R.id.group_text);
        toggleGroup.setOnClickListener(
        		new ToggleButton.OnClickListener()
	        		{
					public void onClick(View v)
						{
						fGroup = !fGroup;
						updateEncoding();
						}
	        		});
        toggleGroup.setChecked(fGroup);
        
        edit.addTextChangedListener(new TextWatcher()
        	{

			public void afterTextChanged(Editable arg0)
				{
				updateEncoding();
				playSound(SoundEffect.KEY_DOWN);
				}

			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
        	});
        
        // Initialize Wikipedia-based info WebView
        
        WebView wv = (WebView) findViewById(R.id.enigma_info);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.loadUrl("http://en.m.wikipedia.org/wiki/Enigma_machine");
        
        // Initialize Settings view
        
        ArrayAdapter<CharSequence> adapter;
        
        adapter = ArrayAdapter.createFromResource(this, R.array.rotor_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        for (int i = 0; i < 3; i++)
        	{
        	aspnRotors[i] = (Spinner) findViewById(R.id.spn_rotors_1 + i);
        	aspnRotors[i].setAdapter(adapter);
        	aspnRotors[i].setSelection(aRotors[i]);
        	
        	aspnRotors[i].setOnItemSelectedListener(
        			// TODO: Send to public method on this class instead of making new
        			// class instance?
            		new OnItemSelectedListener()
            			{
    					public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
    						{
    						int iMe;
    						for (iMe = 0; iMe < 3; iMe++)
    							if (aspnRotors[iMe] == parent)
    								break;
    						
    						// If rotor already used - swap it out with this rotor's value
    						for (int i = 0; i < 3; i++)
	    						{
    							if (i == iMe)
    								continue;
    							 if (aRotors[i] == id)
    							 	{
    								aspnRotors[i].setSelection(aRotors[iMe]);
    								aRotors[i] = aRotors[iMe];
    							 	}
	    						}
    						
    						aRotors[iMe] = (int) id;
    						playSound(SoundEffect.ROTOR);
    						}

    					public void onNothingSelected(AdapterView<?> arg0) {}
            			});
        	}
        	
        adapter = ArrayAdapter.createFromResource(this, R.array.alpha, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        class RotorSettingsListener implements OnItemSelectedListener
            {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
                {
                playSound(SoundEffect.ROTOR);
                }
    
            public void onNothingSelected(AdapterView<?> arg0) {}
            }
        
        RotorSettingsListener listener = new RotorSettingsListener();
        
        for (int i = 0; i < 3; i++)
            {
            aspnRings[i] = (Spinner) findViewById(R.id.spn_rings_1 + i);
            aspnRings[i].setAdapter(adapter);
            aspnRings[i].setOnItemSelectedListener(listener);
            }
        
        for (int i = 0; i < 3; i++)
            {
            aspnStart[i] = (Spinner) findViewById(R.id.spn_start_1 + i);
            aspnStart[i].setAdapter(adapter);
            aspnStart[i].setOnItemSelectedListener(listener);
            }
        
        plugboard = (EditText) findViewById(R.id.plugboard);
        
        updateSettings();
    	}
    
        @Override
        protected void onStart()
            {
            super.onStart();
            Log.d(TAG, "onStart");
            
            if (!fHasMedia)
                {
                mpDown = MediaPlayer.create(this, R.raw.key_down);
                mpUp = MediaPlayer.create(this, R.raw.key_up);
                mpRotor = MediaPlayer.create(this, R.raw.rotor);
                
                Log.d(TAG, "Down: " + (mpDown != null ? mpDown.toString() : "null"));
                Log.d(TAG, "Up: " + (mpUp != null ? mpUp.toString() : "null"));
                Log.d(TAG, "Rotor: " + (mpRotor != null ? mpRotor.toString() : "null"));
                fHasMedia = true;
                }
            }
    
        @Override
        protected void onStop()
            {
            super.onStop();
            Log.d(TAG, "onStop");
            
            if (mpRotor != null)
                {
                mpRotor.release();
                mpRotor = null;
                }
            
            if (mpUp != null)
                {
                mpUp.release();
                mpUp = null;
                }
            
            if (mpDown != null)
                {
                mpDown.release();
                mpDown = null;
                }
            
            fHasMedia = false;
            }
        
        void playSound(SoundEffect snd)
            {
            MediaPlayer mp = null;
            
            switch (snd)
            {
            case KEY_DOWN:
                mp = mpDown;
                break;
            case KEY_UP:
                mp = mpUp;
                break;
            case ROTOR:
                mp = mpRotor;
                break;
            }
            
            if (mp != null)
                {
                Log.d(TAG, "Playing: " + mp.toString());
                mp.seekTo(0);
                mp.start();
                }
            else
                Log.e(TAG, "Sound resource not available!");
            }
    }