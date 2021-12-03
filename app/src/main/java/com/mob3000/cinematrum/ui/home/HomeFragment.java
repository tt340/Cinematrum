package com.mob3000.cinematrum.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mob3000.cinematrum.MainActivity;
import com.mob3000.cinematrum.MovieDetailsActivity;
import com.mob3000.cinematrum.R;
import com.mob3000.cinematrum.dataModels.Category;
import com.mob3000.cinematrum.dataModels.Movie;
import com.mob3000.cinematrum.dataModels.User;
import com.mob3000.cinematrum.databinding.HomeFragment1FragmentBinding;
import com.mob3000.cinematrum.helpers.CategoryAdapter;
import com.mob3000.cinematrum.helpers.LocationTracker;
import com.mob3000.cinematrum.helpers.MovieAdapter;
import com.mob3000.cinematrum.sqlite.DataAcessor;
import com.mob3000.cinematrum.sqlite.DatabaseHelper;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    View root;
    private HomeViewModel homeViewModel;
    private MovieAdapter.MovieClickListener movieClickListener;
    private CategoryAdapter.CategoryClickListener categoryClickListener;
    private RecyclerView movieRecyclerView;
    private RecyclerView categoryRecyclerView;
    private ArrayList<Movie> MovieList;
    private ArrayList<Category> CategoryList;
    private LinearLayoutManager movieLayoutManager;
    private LinearLayoutManager categoryLayoutManager;
    private SearchView searchView;
    private MovieAdapter movieAdapter;
    private CategoryAdapter categoryAdapter;
    private TextView seekBarValue;
    private TextView txtWelcome;
    private SeekBar seekBar;
    private User user;
    private View selectedView;
    private SharedPreferences sp;
    private int selected;
    private ArrayList<Movie> MoviesReplacement;
    private ProgressBar progressBar;
    private TextView txtLoading;
    private HomeFragment1FragmentBinding binding;
    private boolean usingLocationService;
    private Location _location;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        sp = getActivity().getSharedPreferences("login", MODE_PRIVATE);
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = HomeFragment1FragmentBinding.inflate(inflater, container, false);
        root = inflater.inflate(R.layout.fragment_home, container, false);
        movieRecyclerView = root.findViewById(R.id.recyclerViewMovies);
        categoryRecyclerView = root.findViewById(R.id.recyclerViewCategories);
        selectedView = null;
        MovieList = new ArrayList<>();
        CategoryList = new ArrayList<>();
        MoviesReplacement = new ArrayList<>();


        String userMail = sp.getString("email", "default");

        if (userMail != "default") {
            ArrayList<User> users = DataAcessor.getUser(getActivity(), DatabaseHelper.COLUMN_USER_email, userMail);
            if (users.size() == 1)
                user = users.get(0);
        }
        initData();



        setMovieAdapter(MovieList);
        setCategoryAdapter();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Fragment first time opened without location service => Just load movies
        // 2. Fragment first time opened with location service => initialize location tracker and wait for location
        // 3. Fragment second time opened without location service => just load movies
        // 4. Fragment second time opened with location service => restore location out of savedInstanceState and reload movies directly with locationn

        // Reopening Fragment without closing Activity
        MainActivity mainActivity =(MainActivity) getActivity();
        if (mainActivity.usingLocationService && mainActivity._location != null){
            _location = mainActivity._location;
            loadMoviesWithLocation(mainActivity._location);
            turnOffLoading();
        }
        else if (!mainActivity.usingLocationService) {
            loadMoviesWithoutLocation();
            turnOffLoading();
        }
        else if (mainActivity.usingLocationService){
            turnOnLoading();
        }

    }


    private void initData() {
        MovieList = DataAcessor.getMovies(getActivity(), "", "");
        CategoryList = DataAcessor.getCategories(getActivity(), "", "");
        txtWelcome = root.findViewById(R.id.txtWelcome);
        txtWelcome.setText("Welcome, " + user.getName() + " pick a movie");

        //TODO when the slider is moved, take the location and use if for the method that loads all the movies inside that specific radius
        seekBarValue = root.findViewById(R.id.seekBarValue);
        seekBar = root.findViewById(R.id.seekBar);
        searchView = root.findViewById(R.id.searchbar);
        seekBar.setProgress(80);
        seekBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.button_background), android.graphics.PorterDuff.Mode.SRC_IN);
        seekBar.getThumb().setColorFilter(getResources().getColor(R.color.button_background), PorterDuff.Mode.SRC_ATOP);
        seekBarValue.setText(String.valueOf(seekBar.getProgress() + " km"));
        progressBar = root.findViewById(R.id.progressBarLoading);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.button_background), android.graphics.PorterDuff.Mode.SRC_IN);
        txtLoading = root.findViewById(R.id.txtLoading);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if (progress == 100) {
                    seekBarValue.setText(String.valueOf(progress + "+ km"));
                } else {
                    seekBarValue.setText(String.valueOf(progress + " km"));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ArrayList<Movie> newMovies = DataAcessor.getMoviesFromLocation(getActivity(), _location, seekBar.getProgress());
                movieRecyclerView.setAdapter(new MovieAdapter(newMovies, getActivity(), movieClickListener));

            }
        });


    }

    private void setMovieAdapter(ArrayList<Movie> MovieList) {
        setOnClickMovieListener();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                movieAdapter.getFilter().filter(s);
                return false;
            }
        });
        movieAdapter = new MovieAdapter(MovieList, getActivity(), movieClickListener);
        movieLayoutManager = new LinearLayoutManager(getActivity());
        movieLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        movieRecyclerView.setLayoutManager(movieLayoutManager);
        movieRecyclerView.setItemAnimator(new DefaultItemAnimator());
        movieRecyclerView.setAdapter(movieAdapter);

    }

    private void setCategoryAdapter() {
        setOnClickCategoryListener();
        categoryAdapter = new CategoryAdapter(CategoryList, getActivity(), categoryClickListener);
        categoryLayoutManager = new LinearLayoutManager(getActivity());
        categoryLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        categoryRecyclerView.setLayoutManager(categoryLayoutManager);
        categoryRecyclerView.setItemAnimator(new DefaultItemAnimator());
        categoryRecyclerView.setAdapter(categoryAdapter);
    }

    private void setOnClickMovieListener() {
        movieClickListener = new MovieAdapter.MovieClickListener() {
            @Override
            public void onClick(View v, int position) {
                Intent intent = new Intent(getActivity(), MovieDetailsActivity.class);
                intent.putExtra("movieID", MovieList.get(position).getMovie_id());
                intent.putExtra("distance", seekBar.getProgress());
                MainActivity mainActivity = (MainActivity)getActivity();
                intent.putExtra("location", mainActivity._location);
                intent.putExtra("usingLocationService", mainActivity.usingLocationService);
                startActivity(intent);
            }
        };
    }

    private void setOnClickCategoryListener() {
        categoryClickListener = new CategoryAdapter.CategoryClickListener() {
            @Override
            public void onClick(View v, int position) {
                if (selectedView == v) {
                    selectedView.setBackgroundResource(R.drawable.category_background);
                    selectedView = null;
                    movieAdapter.categoryFilter.filter("All");
                } else if (selectedView == null) {
                    selectedView = v;
                    selectedView.setBackgroundResource(R.drawable.category_background_light);
                    movieAdapter.categoryFilter.filter(CategoryList.get(position).getName());
                } else if (selectedView != v && selectedView != null) {
                    v.setBackgroundResource(R.drawable.category_background_light);
                    selectedView.setBackgroundResource(R.drawable.category_background);
                    selectedView = v;
                    movieAdapter.categoryFilter.filter(CategoryList.get(position).getName());
                }
            }
        };
    }


    /*void LoadCategoryImages()
            {
                imageView=root.findViewById(R.id.romanceButton);
                Picasso.get().load("https://thoughtcatalog.com/wp-content/uploads/2013/09/istock_000015777770medium2.jpg").placeholder(R.drawable.category_icons).into(imageView);
                imageView=root.findViewById(R.id.comedyButton);
                Picasso.get().load("https://i.pinimg.com/originals/ef/f8/b9/eff8b9e41133bd9b2b8b733c56b2cbea.jpg").placeholder(R.drawable.category_icons).into(imageView);
                imageView=root.findViewById(R.id.dramaButton);
                Picasso.get().load("https://miro.medium.com/max/1000/1*T-544XBLkxSr4y_aAo5OfQ.jpeg").placeholder(R.drawable.category_icons).into(imageView);
                imageView=root.findViewById(R.id.horrorButton);
                Picasso.get().load("https://i.insider.com/5e5036b5a9f40c18895e8d88?width=700").placeholder(R.drawable.category_icons).into(imageView);
            }*/
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        // TODO: Use the ViewModel
    }


    public void turnOnLoading() {
        progressBar.setVisibility(View.VISIBLE);
        txtLoading.setVisibility(View.VISIBLE);
        movieRecyclerView.setVisibility(View.INVISIBLE);
    }

     public void turnOffLoading() {
        progressBar.setVisibility(View.INVISIBLE);
        txtLoading.setVisibility(View.INVISIBLE);
        movieRecyclerView.setVisibility(View.VISIBLE);
    }

    public void loadMoviesWithLocation(Location loc) {
        ArrayList<Movie> newMovies = DataAcessor.getMoviesFromLocation(getActivity(), loc, seekBar.getProgress());
        movieRecyclerView.setAdapter(new MovieAdapter(newMovies, getActivity(), movieClickListener));
    }

     public void loadMoviesWithoutLocation() {
        ArrayList<Movie> newMovies = DataAcessor.getMovies(getActivity(), "", "");
        movieRecyclerView.setAdapter(new MovieAdapter(newMovies, getActivity(), movieClickListener));
    }


}