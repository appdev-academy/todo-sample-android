package academy.appdev.todolistsample

import academy.appdev.viewextensions.capitalizeFirst
import academy.appdev.viewextensions.isVisible
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.textInputEditText
import org.jetbrains.anko.design.textInputLayout
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


@SuppressLint("ConstantLocale")
val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

class MainActivity : AppCompatActivity() {


    private var adapter: TodosAdapter? = null
    private var savedUid: String? = null

    private val valueListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
            Log.e("Database", "Canceled with error: ${p0.message}")
        }

        override fun onDataChange(p0: DataSnapshot) {
            val list = p0.getValue(object : GenericTypeIndicator<HashMap<String, ToDoItem>>() {})
            adapter?.swapData(list?.toList())
        }
    }

    companion object {
        const val RC_SIGN_IN: Int = 111
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db.setPersistenceEnabled(true)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        if (user != null) {
            db.getReference(user?.uid!!).keepSynced(true)
        }

        adapter = TodosAdapter(this)
        recycler.adapter = adapter


        FirebaseAuth.getInstance().addAuthStateListener {
            onAuthStateChanged(it.currentUser)
        }

        signInButton.onClick {
            startAuthFlow()
        }

        signOutButton.onClick {
            FirebaseAuth.getInstance().signOut()
        }

        fab.onClick {
            showAddListPopup(null, null, ::onSaveClicked)
        }

    }

    fun onSaveClicked(title: String?, id: String?) {
        if (id == null) {
            db.getReference(user?.uid!!).child("ToDos").push().setValue(ToDoItem(title, Date()))
        } else {
            db.getReference(user?.uid!!).child("ToDos").child(id).setValue(ToDoItem(title, Date()))
        }
    }

    private val db get() = FirebaseDatabase.getInstance()

    private fun onAuthStateChanged(currentUser: FirebaseUser?) {
        signInButton.isVisible = currentUser == null
        signOutButton.isVisible = currentUser != null
        if (currentUser == null) {
            adapter?.swapData(null)
            savedUid?.let {
                db.getReference(it).child("ToDos")
                    .removeEventListener(valueListener)
            }
        } else {
            savedUid = currentUser.uid
            db.getReference(currentUser.uid).child("ToDos")
                .addValueEventListener(valueListener)
        }
    }


    val user get() = FirebaseAuth.getInstance().currentUser


    data class ToDoItem(
        var title: String? = null,
        var dueDate: Date? = null
    )

    private fun startAuthFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(), RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                Log.d("Sign in", "Success")

            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Log.e("Sign in", "Failed")
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun onItemClicked(id: String, item: ToDoItem) {
        selector(item.title, listOf("Edit", "Remove")) { di, index ->
            if (index == 0) showAddListPopup(item, id, ::onSaveClicked)
            if (index == 1) db.getReference(user?.uid!!).child("ToDos").child(id).removeValue()
        }
    }

    fun showAddListPopup(
        todo: ToDoItem?,
        id: String?,
        onSaveClicked: (String?, String?) -> Unit
    ) {
        var titleInput: TextInputEditText? = null
        alert {
            customView {
                verticalLayout {
                    padding = dip(16)
                    textView {
                        textResource = if (todo == null) R.string.add_new else R.string.edit
                        textSize = 18f
                    }.lparams {
                        gravity = android.view.Gravity.CENTER_HORIZONTAL
                    }
                    textInputLayout {
                        hint = ctx.getString(R.string.title)
                        titleInput = textInputEditText {
                            capitalizeFirst()
                            setText(todo?.title)
                        }
                    }
                }
            }
            positiveButton(android.R.string.ok) {
                val newTitle = titleInput?.text?.toString()
                onSaveClicked(newTitle, id)
            }
            negativeButton(android.R.string.cancel) {}
        }.show()
    }

}




