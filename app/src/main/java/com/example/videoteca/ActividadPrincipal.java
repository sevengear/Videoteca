package com.example.videoteca;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.media.tv.Channel;
import android.support.media.tv.ChannelLogoUtils;
import android.support.media.tv.TvContractCompat;

/**
 * Created by Miguel Á. Núñez on 22/06/2018.
 */
public class ActividadPrincipal extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        crearCanal(getApplicationContext(), "Videoteca");
    }

    private long existeCanal(Context context, String canal) {
        Cursor cursor = context.getContentResolver()
                .query(TvContractCompat.Channels.CONTENT_URI,
                        new String[]{
                                TvContractCompat.Channels._ID,
                                TvContract.Channels.COLUMN_DISPLAY_NAME},
                        null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Channel channel = Channel.fromCursor(cursor);
                if (canal.equals(channel.getDisplayName())) {
                    return channel.getId();
                }
            } while (cursor.moveToNext());
        }
        return -1L;
    }

    private void crearCanal(Context context, String canal) {
        long channelId = existeCanal(getApplicationContext(), canal);
        if (channelId == -1) {
            Uri appLinkIntentUri = Uri.parse("tvrecomendation://principal");
            Channel.Builder builder = new Channel.Builder();
            builder.setType(TvContractCompat.Channels.TYPE_PREVIEW).setDisplayName(canal).setAppLinkIntentUri(appLinkIntentUri);
            Uri channelUri = getApplicationContext().getContentResolver().insert(TvContractCompat.Channels.CONTENT_URI, builder.build().toContentValues());
            channelId = ContentUris.parseId(channelUri);
            crearLogoCanal(getApplicationContext(), channelId, R.drawable.app_icon_your_company);
            TvContractCompat.requestChannelBrowsable(getApplicationContext(), channelId);
        } else {
            Utils.idCanalPredeterminado = channelId;
        }
    }

    private void crearLogoCanal(Context context, long channelId, @DrawableRes int drawableId) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawableId);
        ChannelLogoUtils.storeChannelLogo(context, channelId, bitmap);
    }
}
