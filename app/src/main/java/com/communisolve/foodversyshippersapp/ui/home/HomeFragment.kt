package com.communisolve.foodversyshippersapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.communisolve.foodversyshippersapp.R
import com.communisolve.foodversyshippersapp.adapter.MyShippingOrdersAdapter
import com.communisolve.foodversyshippersapp.common.Common
import com.communisolve.foodversyshippersapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var layoutAnimationController: LayoutAnimationController
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_item_from_left)
        binding.recyclerOrder.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
        }

        homeViewModel.messageError.observe(viewLifecycleOwner, Observer {
            if (isAdded) {
                Toast.makeText(requireContext(), "$it", Toast.LENGTH_SHORT).show()
            }
        })

        homeViewModel.getOrderModelMutableLiveData(Common.currentShipperUser!!.phone)
            .observe(viewLifecycleOwner, Observer { shippingOrderslist ->
                binding.recyclerOrder.adapter =
                    MyShippingOrdersAdapter(requireContext(), shippingOrderslist)
                binding.recyclerOrder.layoutAnimation = layoutAnimationController
            })
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}