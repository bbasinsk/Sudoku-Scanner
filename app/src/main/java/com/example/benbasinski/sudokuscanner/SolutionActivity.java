package com.example.benbasinski.sudokuscanner;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.HashSet;

public class SolutionActivity extends AppCompatActivity {

    char[] solutions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solution);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        Bundle extras = getIntent().getExtras();
        int[] pre_solved = extras.getIntArray("DIGITS");

        if (pre_solved == null) {
            throw new NullPointerException("No puzzle passed");
        }

        findSolution(pre_solved);

        //Display result
        TextView[] textViews = new TextView[solutions.length];
        for (int i = 0; i < textViews.length; i++) {
            String name = "digit"+(i+1);
            int id = getResources().getIdentifier(name, "id", getPackageName());
            textViews[i] = (TextView) findViewById(id);
            textViews[i].setText(String.valueOf(solutions[i]));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    private void findSolution(int[] pre_solved) {
        solutions = new char[81];
        char[][] board = new char[9][9];
        for (int i = 0; i < pre_solved.length; i++) {
            char c;
            if (pre_solved[i] == 0) {
                c = '.';
            } else {
                c = (char) (pre_solved[i] + 48);
            }
            board[i/9][i%9] = c;
        }
        solveSudoku(board);
        for (int i = 0; i < 81; i++) {
            solutions[i] = board[i/9][i%9];
        }
    }

    public void solveSudoku(char[][] board) {
        solve(board);
    }

    public boolean solve(char[][] board){
        for(int i=0; i<9; i++){
            for(int j=0; j<9; j++){
                if(board[i][j]!='.')
                    continue;

                for(int k=1; k<=9; k++){
                    board[i][j] = (char) (k+'0');
                    if(isValid(board, i, j) && solve(board))
                        return true;
                    board[i][j] = '.';
                }

                return false;
            }
        }

        return true; // does not matter
    }

    public boolean isValid(char[][] board, int i, int j){
        HashSet<Character> set = new HashSet<Character>();

        for(int k=0; k<9; k++){
            if(set.contains(board[i][k]))
                return false;

            if(board[i][k]!='.' ){
                set.add(board[i][k]);
            }
        }

        set.clear();

        for(int k=0; k<9; k++){
            if(set.contains(board[k][j]))
                return false;

            if(board[k][j]!='.' ){
                set.add(board[k][j]);
            }
        }

        set.clear();

        for(int m=0; m<3; m++){
            for(int n=0; n<3; n++){
                int x=i/3*3+m;
                int y=j/3*3+n;
                if(set.contains(board[x][y]))
                    return false;

                if(board[x][y]!='.'){
                    set.add(board[x][y]);
                }
            }
        }

        return true;
    }

}
