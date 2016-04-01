package com.stuffthatworks.teamcalendar.util;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;

import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.stuffthatworks.teamcalendar.interfaces.AuthenticatorListener;

/**
 * Singleton class that handles authentication (Google, Facebook etc.)
 */
public class Authenticator {
    /* Singleton */
    private static Authenticator singleton = new Authenticator();

    private Authenticator() {
    }

    private static Activity thisActivity;
    private static final int RC_SIGN_IN = 0;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";
    private boolean mResolvingError = false;
    private GoogleSignInAccount googleSignInAccount;

    public static Authenticator getInstance() {
        return singleton;
    }

    /* Handle event listeners */
    private ArrayList<AuthenticatorListener> listeners = new ArrayList<AuthenticatorListener>();

    public void addListener(AuthenticatorListener listener) {
        listeners.add(listener);
    }

    private void onAuthenticationFailure() {
        for (AuthenticatorListener listener :
                listeners) {
            listener.onAuthenticationFailure();
        }
    }

    private void onAuthenticationSuccess() {
        AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
        builder.setTitle(googleSignInAccount.getDisplayName());
        builder.setMessage(googleSignInAccount.getEmail());
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.show();

        for (AuthenticatorListener listener :
                listeners) {
            listener.onAuthenticationSuccess();
        }
    }

    /**
     *
     */
    private GoogleApiClient mGoogleApiClient;

    public void init(final Activity activity) {
        thisActivity = activity;

        //Configure sign-in to request the user's ID, email ID and basic profile.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();

        //Build a GoogleApiClient with access to the Google Sign-In API and the options specified by the gso.
        mGoogleApiClient = new GoogleApiClient.Builder(thisActivity).addApi(Auth.GOOGLE_SIGN_IN_API, gso).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {

            }

            @Override
            public void onConnectionSuspended(int i) {

            }
        }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                if (mResolvingError) {
                    // Already attempting to resolve an error.
                    return;
                } else if (connectionResult.hasResolution()) {
                    try {
                        mResolvingError = true;
                        connectionResult.startResolutionForResult(activity, REQUEST_RESOLVE_ERROR);
                    } catch (IntentSender.SendIntentException e) {
                        // There was an error with the resolution intent. Try again.
                        mGoogleApiClient.connect();
                    }
                } else {
                    // No resolution possible!
                    showErrorDialog(connectionResult.getErrorCode());
                    mResolvingError = false;
                    onAuthenticationFailure();
                }
            }
        }).build();
        mGoogleApiClient.connect();

        //Start a silent-sign in. Is the user already signed in?
        OptionalPendingResult<GoogleSignInResult> pendingResult =
                Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (pendingResult.isDone()) {
            // There's immediate result available.
            handleSilentSignInResult(pendingResult.get());
        } else {
            // There's no immediate result ready, waits for the async callback.
            pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult result) {
                    handleSilentSignInResult(result);
                }
            });
        }
    }

    public void authenticateWithGoogle() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        thisActivity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * This method should be called from the intent that receives the 'result' of the authentication activity.
     * This method checks for a response in the intent data and raises the authentication success/failure
     * callback accordingly.
     *
     * If the passed intent is not relevant to authentication, the method does nothing.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void checkAuthenticationActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == RC_SIGN_IN) {
            GoogleSignInResult signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if(signInResult.isSuccess()) {
                googleSignInAccount = signInResult.getSignInAccount();
                onAuthenticationSuccess();
            } else {
                googleSignInAccount = null;
                onAuthenticationFailure();
            }
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();

        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(thisActivity.getFragmentManager(), "error_dialog");
    }

    private void handleSilentSignInResult(GoogleSignInResult result) {
        if(result.isSuccess()) {
            googleSignInAccount = result.getSignInAccount();
            onAuthenticationSuccess();
        } else {
            googleSignInAccount = null;
            onAuthenticationFailure();
        }
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }
    }
}
