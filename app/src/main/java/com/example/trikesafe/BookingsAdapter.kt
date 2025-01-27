package com.example.trikesafe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookingsAdapter(private val onBookingClick: (Booking) -> Unit) :
    RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {

    private var bookings: List<Booking> = listOf()

    class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bookingId: TextView = view.findViewById(R.id.bookingId)
        val pickupLocation: TextView = view.findViewById(R.id.pickupLocation)
        val dropoffLocation: TextView = view.findViewById(R.id.dropoffLocation)
        val fare: TextView = view.findViewById(R.id.fare)
        val viewButton: Button = view.findViewById(R.id.viewButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.booking_item, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bookingId.text = "Booking #${booking.id}"
        holder.pickupLocation.text = "Pickup: ${booking.pickup_location}"
        holder.dropoffLocation.text = "Dropoff: ${booking.dropoff_location}"
        holder.fare.text = "Fare: ₱${String.format("%.2f", booking.total_fare)}"

        holder.viewButton.setOnClickListener {
            onBookingClick(booking)
        }
    }

    override fun getItemCount() = bookings.size

    fun updateBookings(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
}