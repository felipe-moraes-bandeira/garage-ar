package com.ifpr.androidapptemplatear

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.ifpr.androidapptemplatear.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment

        // Os ids do menu (AR, Modelos, Garagem) coincidem com os destinos do nav_graph.
        val navController = navHost.navController
        binding.navView.setupWithNavController(navController)

        // Telas internas (Placar, Detalhes, Formulário, Mapa) não são abas: a aba de origem continua
        // marcada e tocar nela contava como "reselecionar", que o Navigation ignora, e o usuário
        // ficava preso. Reselecionar agora volta para a tela inicial da aba.
        binding.navView.setOnItemReselectedListener { item ->
            navController.popBackStack(item.itemId, false)
        }
    }
}
