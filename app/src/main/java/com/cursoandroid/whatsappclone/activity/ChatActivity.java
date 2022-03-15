package com.cursoandroid.whatsappclone.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cursoandroid.whatsappclone.R;
import com.cursoandroid.whatsappclone.adapter.MensagensAdapter;
import com.cursoandroid.whatsappclone.config.ConfiguracaoFirebase;
import com.cursoandroid.whatsappclone.helper.Base64Custom;
import com.cursoandroid.whatsappclone.helper.UsuarioFirebase;
import com.cursoandroid.whatsappclone.model.Conversa;
import com.cursoandroid.whatsappclone.model.Mensagem;
import com.cursoandroid.whatsappclone.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;


public class ChatActivity extends AppCompatActivity {
    private TextView textViewNome;
    private CircleImageView circleImageViewFoto;
    private EditText editMensagem;
    private ImageView imageCamera;
    private Usuario usuarioDestinatario;
    private static final int SELECAO_CAMERA= 100;
    //identificador usuarios remetente e destinatario
    private String idUsuarioRemetente;
    private String idUsuarioDestinatario;

    private RecyclerView recyclerMensagens;
    private MensagensAdapter adapter;
    private List<Mensagem> mensagems = new ArrayList<>();
    private DatabaseReference database;
    private DatabaseReference mensagensRef;
    private StorageReference storage;
    private ChildEventListener childEventListenerMensagens;




      @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Configurações iniciais
        textViewNome = findViewById(R.id.textViewNomeChat);
        circleImageViewFoto= findViewById(R.id.circlImageViewFotoPerfil);
        editMensagem = findViewById(R.id.editMensagem);
        recyclerMensagens = findViewById(R.id.recyclerMensagens);
        imageCamera = findViewById(R.id.imageCamera);

        //recupera dados do usuario remetente
          idUsuarioRemetente = UsuarioFirebase.getIdentificadorUsuario();

        //Recuperar dados do usuario destinatario
          Bundle bundle = getIntent().getExtras();
          if(bundle != null){
            usuarioDestinatario = (Usuario) bundle.getSerializable("chatContato");
            textViewNome.setText(usuarioDestinatario.getNome());
          }else{
              circleImageViewFoto.setImageResource(R.drawable.padrao);
          }
          //recuperar dados usuario destinatario
          idUsuarioDestinatario = Base64Custom.codificarBase64(usuarioDestinatario.getEmail());

           //configurar adapter
          adapter = new MensagensAdapter(mensagems,getApplicationContext());
          //Configuração recyclerview
          RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
          recyclerMensagens.setLayoutManager(layoutManager);
          recyclerMensagens.setHasFixedSize( true );
          recyclerMensagens.setAdapter(adapter);
          database = ConfiguracaoFirebase.getFirebaseReference();
          storage = ConfiguracaoFirebase.getFirebaseStorage();
          mensagensRef = database.child("mensagens")
                  .child(idUsuarioRemetente)
                  .child(idUsuarioDestinatario);

          // evento de clique na camera
          imageCamera.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                  Intent i =new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                  if(i.resolveActivity(getPackageManager())!=null){
                      startActivityForResult(i,SELECAO_CAMERA);
                  }
              }
          });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            Bitmap imagem = null;
            try{
                switch (requestCode){
                    case SELECAO_CAMERA:
                        imagem = (Bitmap) data.getExtras().get("data");
                        break;
                }
                if (imagem != null){
                    //Recuperar imagem no Firebase
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG,70,baos);
                    byte[] dadosImagem = baos.toByteArray();

                    //Criar nome da imagem
                    String nomeImagem = UUID.randomUUID().toString();

                    //configurando referencia do firebae
                    final StorageReference imagemRef = storage.child("imagens")
                            .child("fotos")
                            .child(idUsuarioRemetente)
                            .child(nomeImagem);
                    UploadTask uploadTask = imagemRef.putBytes(dadosImagem);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("Erro","Erro ao fazer upload");
                            Toast.makeText(ChatActivity.this,
                                    "Erro ao fazer upload ",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                imagemRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        Uri url = task.getResult();
                                        Mensagem mensagem = new Mensagem();
                                        mensagem.setIdUsuario(idUsuarioRemetente);
                                        mensagem.setMensagem("imagem.jpeg");
                                        mensagem.setImagem(url);

                                        //salvar imagem para o remetente
                                        salvarMensagem(idUsuarioRemetente, idUsuarioDestinatario,mensagem);
                                        //salvar mensagem para o destinatario
                                        salvarMensagem(idUsuarioDestinatario,idUsuarioRemetente,mensagem);
                                        Toast.makeText(ChatActivity.this,
                                                "Sucesso ao enviar imagem",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                        }
                    });

                }
            }catch (Exception e ){
                e.printStackTrace();
            }
        }
    }

    public void enviarMensagem(View view){

        String textoMensagem = editMensagem.getText().toString();
        if(!textoMensagem.isEmpty()){
            Mensagem mensagem = new Mensagem();
            mensagem.setMensagem(idUsuarioRemetente);
            mensagem.setMensagem(textoMensagem);

            //Salvar mensagem para o remetente
            salvarMensagem(idUsuarioRemetente,idUsuarioDestinatario,mensagem);

            //Salvar mensagem para o destinatario
            salvarMensagem(idUsuarioDestinatario,idUsuarioRemetente,mensagem);

            //salvar conversa
            salvarConversa(mensagem);

        }else{
            Toast.makeText(ChatActivity.this,
                    "Digite uma mensagem para enviar",
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void salvarConversa(Mensagem msg){

        Conversa conversaRemetente = new Conversa();
        conversaRemetente.setIdRemetente(idUsuarioRemetente);
        conversaRemetente.setIdDestinatario(idUsuarioDestinatario);
        conversaRemetente.setUltimaMensagem(msg.getMensagem());
        conversaRemetente.setUsuarioExibicao(usuarioDestinatario);
        conversaRemetente.salvar();


    }
    private void salvarMensagem(String idRemetente, String idDestinatario, Mensagem msg){
        DatabaseReference database = ConfiguracaoFirebase.getFirebaseReference();
        mensagensRef = database.child("mensagens");
        mensagensRef
                    .child(idRemetente)
                    .child(idDestinatario)
                    .push()
                    .setValue(msg);



        //Limpar o texto
        editMensagem.setText("");
    }

    @Override
    protected void onStart() {
          recuperarMensagens();
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mensagensRef.removeEventListener(childEventListenerMensagens);
    }

    public void recuperarMensagens(){
        childEventListenerMensagens = mensagensRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Mensagem mensagem = snapshot.getValue(Mensagem.class);
                mensagems.add(mensagem);
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

}