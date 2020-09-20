package com.rogok.working_with_firebase

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val REQUEST_CODE_IMAGE_PICK = 0


class MainActivity : AppCompatActivity() {

    var curFile: Uri? = null
    val imageRef = Firebase.storage.reference
    val imageList: ArrayList<Uri> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_select.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(
                Intent.createChooser(intent, "Select images"),
                REQUEST_CODE_IMAGE_PICK
            )

        }

        btn_upload_image.setOnClickListener {
            uploadImageToStorage(imageList)
        }

        listFiles()
    }

    private fun listFiles() = CoroutineScope(Dispatchers.IO).launch {
        try {

            val images = imageRef.child("images/").listAll().await()
            val imageUrls = mutableListOf<String>()
            for (image in images.items) {
                val url = image.downloadUrl.await()
                imageUrls.add(url.toString())
            }
            withContext(Dispatchers.Main) {
                val imageAdapter = ImageAdapter(imageUrls)
                rvImages.apply {
                    adapter = imageAdapter
                    layoutManager = LinearLayoutManager(this@MainActivity)
                }
            }

        } catch (e : Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    e.message,
                    Toast.LENGTH_LONG
                ).show()
            }

        }
    }

    private fun uploadImageToStorage(imageList: ArrayList<Uri>) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (imageList.size == 1) {

                    curFile?.let {
                        imageRef.child("images/${imageList[0].lastPathSegment}").putFile(it).await()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "Successfully upload image",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                else {
                    var i = 0
                    while (i < imageList.size) {
                        imageList[i].lastPathSegment?.let {
                            imageRef.child("images/${imageList[i].lastPathSegment}").putFile(imageList[i])
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Successfully upload image",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }.addOnProgressListener {
                                    val progress = (100.0 * it.bytesTransferred) / it.totalByteCount
                                    Log.d("progress", "Upload is $progress% done")
                                }
                        }
                        i++
                    }
                }


            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_IMAGE_PICK && data?.clipData != null) {
            val count = data.clipData?.itemCount  // number of images
            Log.d("count", count.toString())
            var i = 0
            while (i < count!! ) {
                curFile = data.clipData?.getItemAt(i)?.uri
                imageList.add(curFile!!) //write images uris to list
                i++

            }
                Toast.makeText(this, "$count", Toast.LENGTH_SHORT).show()


        } else {
            data?.data?.let {
                curFile = it // write uri of a single image
                imageList.add(curFile!!)
                Toast.makeText(this, "1", Toast.LENGTH_SHORT).show()

            }
        }
    }
}





