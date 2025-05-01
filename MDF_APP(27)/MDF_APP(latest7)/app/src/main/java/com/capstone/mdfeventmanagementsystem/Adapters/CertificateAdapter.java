package com.capstone.mdfeventmanagementsystem.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

    @Override
    public CertificateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cert, parent, false);
        return new CertificateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CertificateViewHolder holder, int position) {
        Certificate certificate = certificateList.get(position);

        // Load certificate preview image
        Glide.with(context)
                .load(certificate.getPreviewImageUrl())
                .placeholder(R.drawable.cert_nav)
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

        public CertificateViewHolder(View itemView) {
            super(itemView);
            certPreview = itemView.findViewById(R.id.certPreview);
            certificateName = itemView.findViewById(R.id.certificateName);
            receivedDate = itemView.findViewById(R.id.receivedDate);
        }
    }
}
