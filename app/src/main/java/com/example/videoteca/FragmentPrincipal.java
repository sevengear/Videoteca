package com.example.videoteca;

import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.media.tv.PreviewProgram;
import android.support.media.tv.TvContractCompat;
import android.support.media.tv.WatchNextProgram;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Miguel Á. Núñez on 22/06/2018.
 */
public class FragmentPrincipal extends BrowseFragment {
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        leerDatos();
        iniciarInterfazUsuario();
        cargarListas();
        setupEventListeners();
    }

    private void leerDatos() {
        MovieList.list = new ArrayList<>();
        String json = Utils.loadJSONFromResource(getActivity(), R.raw.movies);
        Gson gson = new Gson();
        Type collection = new TypeToken<ArrayList<Movie>>() {
        }.getType();
        MovieList.list = gson.fromJson(json, collection);
    }

    private void iniciarInterfazUsuario() {
        setTitle("Videoteca");
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(getResources().getColor(R.color.fastlane_background));
        setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));
    }

    private void cargarListas() {
        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        CardPresenter cardPresenter = new CardPresenter();
        List<String> categories = getCategories();
        if (categories == null || categories.isEmpty()) return;
        for (String category : categories) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
            for (Movie movie : MovieList.list) {
                if (category.equalsIgnoreCase(movie.getCategory())) {
                    listRowAdapter.add(movie);
                    añadirPrograma(Utils.idCanalPredeterminado, movie);
                    añadirProgramaWhatchNext(movie);
                }
            }
            if (listRowAdapter.size() > 0) {
                HeaderItem header = new HeaderItem(rowsAdapter.size() - 1, category);
                rowsAdapter.add(new ListRow(header, listRowAdapter));
            }
        }
        listaPreferencias(rowsAdapter);
        setAdapter(rowsAdapter);
    }

    private List<String> getCategories() {
        if (MovieList.list == null) return null;
        List<String> categories = new ArrayList<String>();
        for (Movie movie : MovieList.list) {
            if (!categories.contains(movie.getCategory())) {
                categories.add(movie.getCategory());
            }
        }
        return categories;
    }

    private void listaPreferencias(ArrayObjectAdapter adapter) {
        HeaderItem gridHeader = new HeaderItem(adapter.size() - 1, "PREFERENCIAS");
        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add("Vistas");
        gridRowAdapter.add("Errores");
        gridRowAdapter.add("Preferencias");
        adapter.add(new ListRow(gridHeader, gridRowAdapter));
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(getResources().getColor(R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Movie) {
                Movie movie = (Movie) item;
                Intent intent = new Intent(getActivity(), ActividadDetalles.class);
                intent.putExtra(ActividadDetalles.MOVIE, movie);
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), ((ImageCardView) itemViewHolder.view).getMainImageView(), ActividadDetalles.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            } else if (item instanceof String) {
                Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void añadirPrograma(long channelId, Movie movie) {
        PreviewProgram.Builder builder = new PreviewProgram.Builder();
        builder.setChannelId(channelId)
                .setType(TvContractCompat.PreviewPrograms.TYPE_CLIP)
                .setTitle(movie.getTitle())
                .setDescription(movie.getDescription())
                .setPosterArtUri(Uri.parse(movie.getCardImageUrl()))
                .setIntentUri(
                        Uri.parse("tvrecomendation://detalles?id=" + movie.getId()))
                .setInternalProviderId(String.valueOf(movie.getId()));
        Uri programUri = getContext().getContentResolver().insert(
                TvContractCompat.PreviewPrograms.CONTENT_URI,
                builder.build().toContentValues());
    }

    private void añadirProgramaWhatchNext(Movie movie) {
        WatchNextProgram.Builder builder = new WatchNextProgram.Builder();
        builder.setType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_WATCHLIST)
                .setTitle(movie.getTitle())
                .setDescription(movie.getDescription())
                .setPosterArtUri(Uri.parse(movie.getCardImageUrl()))
                .setIntentUri(
                        Uri.parse("tvrecomendation://detalles/movie?id=" + movie.getId()))
                .setInternalProviderId(String.valueOf(movie.getId()));
        Uri programUri = getContext().getContentResolver().insert(
                TvContractCompat.PreviewPrograms.CONTENT_URI,
                builder.build().toContentValues());
        long programId = ContentUris.parseId(programUri);
    }
}