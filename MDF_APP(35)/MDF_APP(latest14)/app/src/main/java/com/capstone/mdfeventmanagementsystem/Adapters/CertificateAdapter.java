package com.capstone.mdfeventmanagementsystem.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.Certificate;
import com.capstone.mdfeventmanagementsystem.Student.StudentCertificateInside;

import java.util.List;

public class CertificateAdapter extends RecyclerView.Adapter<CertificateAdapter.CertificateViewHolder> {

    private final Context context;
    private final List<Certificate> certificateList;

    public CertificateAdapter(Context context, List<Certificate> certificateList) {
        this.context = context;
        this.certificateList = certificateList;
    }

    @NonNull
    @Override
    public CertificateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cert, parent, false);
        return new CertificateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CertificateViewHolder holder, int position) {
        Certificate certificate = certificateList.get(position);

        // Show loading indicator
        holder.progressBar.setVisibility(View.VISIBLE);

        // Set up request options to maintain aspect ratio
        RequestOptions requestOptions = new RequestOptions()
                .fitCenter()  // This centers the image and maintains aspect ratio
                .override(Target.SIZE_ORIGINAL)  // Use original image dimensions
                .dontTransform();  // Don't apply any transformations that might distort the image

        // Load certificate preview image with proper handling
        Glide.with(context)
                .load(certificate.getPreviewImageUrl())
                .apply(requestOptions)
                .placeholder(R.drawable.cert_nav)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(holder.certPreview);

        holder.certificateName.setText(certificate.getTemplateName());
        holder.receivedDate.setText("Received: " + certificate.getReceivedDate());

        // Handle item click and pass certificate key
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, StudentCertificateInside.class);
            intent.putExtra("certificateKey", certificate.getCertificateKey());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return certificateList.size();
    }

    public static class CertificateViewHolder extends RecyclerView.ViewHolder {
        ImageView certPreview;
        TextView certificateName, receivedDate;
        ProgressBar progressBar;

        public CertificateViewHolder(View itemView) {
            super(itemView);
            certPreview = itemView.findViewById(R.id.certPreview);
            certificateName = itemView.findViewById(R.id.certificateName);
            receivedDate = itemView.findViewById(R.id.receivedDate);
            progressBar = itemView.findViewById(R.id.certLoadingProgress);
        }
    }
}