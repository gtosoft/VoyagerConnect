package com.gtosoft.voyager;

import android.app.Activity;
import android.os.Bundle;

/**
 * 
 Next steps:

  mkdir VoyagerConnect
  cd VoyagerConnect
  git init
  touch README
  git add README
  git commit -m 'first commit'
  git remote add origin git@github.com:gtosoft/VoyagerConnect.git
  git push -u origin master
      
 *
 */

public class ConnectUI extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}