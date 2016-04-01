package com.stuffthatworks.teamcalendar;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.stuffthatworks.teamcalendar.interfaces.AuthenticatorListener;
import com.stuffthatworks.teamcalendar.util.Authenticator;

public class LoginActivity extends Activity implements AuthenticatorListener, View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        findViewById(R.id.google_sign_in_button).setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAuthenticationSuccess() {
        (findViewById(R.id.login_progressBar)).setVisibility(View.INVISIBLE);
        finish();
    }

    @Override
    public void onAuthenticationFailure() {
        (findViewById(R.id.login_progressBar)).setVisibility(View.INVISIBLE);
        //TODO: Show error message here.
    }

    /**
     * Event handler for click on the 'Sign in with Google' button
     * @param view
     */
    public void onClick(View view) {
        //TODO: First check which button was clicked :-)
        (findViewById(R.id.login_progressBar)).setVisibility(View.VISIBLE);
        Authenticator.getInstance().authenticateWithGoogle();
        Authenticator.getInstance().addListener(this);
    }
}
