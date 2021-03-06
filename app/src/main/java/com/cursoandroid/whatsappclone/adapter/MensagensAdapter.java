package com.cursoandroid.whatsappclone.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cursoandroid.whatsappclone.R;
import com.cursoandroid.whatsappclone.helper.UsuarioFirebase;
import com.cursoandroid.whatsappclone.model.Mensagem;

import java.util.List;

public class MensagensAdapter extends RecyclerView.Adapter<MensagensAdapter.MyViewHolder> {
        private List<Mensagem> mensagems;
        private Context context;
        private static final int TIPO_REMETENTE = 0;
        private static final int TIPO_DESTINATARIO = 1;

    public MensagensAdapter(List<Mensagem> lista, Context c) {
        this.mensagems = lista;
        this.context = c;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = null;
        if(viewType == TIPO_REMETENTE){
            item = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_mensagem_remetente,
            parent, false
            );
        }else if (viewType == TIPO_DESTINATARIO){
            item = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_mensagem_destinatario,
                    parent, false
            );
        }
        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Mensagem mensagem = mensagems.get(position);
        String msg = mensagem.getMensagem();
        String imagem = mensagem.getImagem();
    }

    @Override
    public int getItemCount() {
        return mensagems.size();
    }

    @Override
    public int getItemViewType(int position) {
        Mensagem mensagem = mensagems.get(position);
        String idUsuario = UsuarioFirebase.getIdentificadorUsuario();

        if (idUsuario.equals(mensagem.getIdUsuario())){
            return TIPO_REMETENTE;
        }
        return TIPO_DESTINATARIO;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        TextView mensagem;
        ImageView imagem;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            mensagem = itemView.findViewById(R.id.textMensagemTexto);
            imagem = itemView.findViewById(R.id.imageMensagemFoto);
        }
    }
}
