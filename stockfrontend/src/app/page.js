'use client';
import { useState, useEffect } from 'react';
import { orderApi } from '@/services/api';

export default function Home() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeOrders, setActiveOrders] = useState(0);
  const [totalLentItems, setTotalLentItems] = useState(0);
  const [recentOrders, setRecentOrders] = useState([]);

  useEffect(() => {
    const fetchDashboardData = async () => {
      setLoading(true);
      try {
        // Fetch lent orders to get active count
        const lentResponse = await orderApi.getLentOrders(0, 1000, 'timestamp', 'desc');
        const activeOrdersCount = lentResponse.content.filter(order => order.status === 'active').length;
        setActiveOrders(activeOrdersCount);
        
        // Calculate total lent items from active orders
        const totalItems = lentResponse.content
          .filter(order => order.status === 'active')
          .reduce((sum, order) => sum + 1, 0);
        setTotalLentItems(totalItems);

        // Fetch recent orders (sales and lent)
        const [salesData, lentData] = await Promise.all([
          orderApi.getSalesOrders(0, 10, 'timestamp', 'desc'),
          orderApi.getLentOrders(0, 10, 'timestamp', 'desc')
        ]);

        // Combine and sort recent orders
        const allRecentOrders = [
          ...(salesData.content || []).map(order => ({ ...order, type: 'sales' })),
          ...(lentData.content || []).map(order => ({ ...order, type: 'lent' }))
        ]
        .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
        .slice(0, 10); // Get only the 10 most recent orders

        setRecentOrders(allRecentOrders);
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
        setError(err.message || 'Failed to fetch dashboard data');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  // Function to get status display
  const getStatusDisplay = (record) => {
    if (record.type === 'lent') {
      return record.status || '-';
    }
    return 'Sold';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-700">Loading dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-red-600">{error}</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold text-black">Dashboard</h1>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Active Orders Card */}
        <div className="bg-white shadow rounded-lg p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-2">Active Orders</h2>
          <p className="text-3xl font-bold text-blue-600">{activeOrders}</p>
          <p className="text-sm text-gray-600 mt-1">Currently active lent orders</p>
        </div>

        {/* Total Lent Items Card */}
        <div className="bg-white shadow rounded-lg p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-2">Total Lent Items</h2>
          <p className="text-3xl font-bold text-green-600">{totalLentItems}</p>
          <p className="text-sm text-gray-600 mt-1">Total items currently lent out</p>
        </div>
      </div>

      {/* Recent Orders */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">Recent Orders</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Type</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Order ID</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Employee ID</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Shop Name</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Date</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {recentOrders.length === 0 ? (
                <tr>
                  <td className="px-6 py-4 text-gray-700 text-center" colSpan="6">
                    No recent orders found
                  </td>
                </tr>
              ) : (
                recentOrders.map((order, index) => (
                  <tr key={`${order.orderId}-${index}`} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700 capitalize">
                      {order.type}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {order.orderId}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {order.employeeId || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {order.shopName || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {getStatusDisplay(order)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {new Date(order.timestamp).toLocaleString('th-TH', {
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                        hour12: false
                      })}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
