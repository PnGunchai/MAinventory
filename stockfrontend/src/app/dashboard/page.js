'use client';
import { useState, useEffect } from 'react';
import { orderApi } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import { formatDateTime } from '@/utils/dateUtils';
import { useTranslation } from 'react-i18next';

export default function Dashboard() {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeOrders, setActiveOrders] = useState(0);
  const [totalLentItems, setTotalLentItems] = useState(0);
  const [recentOrders, setRecentOrders] = useState([]);
  const user = useAuthStore(state => state.user);

  useEffect(() => {
    const fetchDashboardData = async () => {
      setLoading(true);
      try {
        // Fetch lent orders to get active count
        const lentResponse = await orderApi.getLentOrders(0, 1000, 'timestamp', 'desc');
        const activeOrders = lentResponse.content.filter(order => order.status === 'active');
        setActiveOrders(activeOrders.length);
        
        // Fetch items for each active order and count items with lent status
        let totalLentItems = 0;
        for (const order of activeOrders) {
          try {
            const items = await orderApi.getLentOrderItems(order.lentId);
            if (items && Array.isArray(items)) {
              const lentItemsCount = items.filter(item => 
                item.status === 'active' || item.status === 'lent'
              ).length;
              totalLentItems += lentItemsCount;
            }
          } catch (err) {
            console.error(`Error fetching items for order ${order.lentId}:`, err);
          }
        }
        setTotalLentItems(totalLentItems);

        // Get active lent orders for the table
        setRecentOrders(activeOrders.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp)));
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
        setError(err.message || 'Failed to fetch dashboard data');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-700">{t('loadingDashboard')}</p>
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
      <h1 className="text-3xl font-bold text-black">{t('dashboard')}</h1>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Active Orders Card */}
        <div className="bg-white shadow rounded-lg p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-2">{t('activeOrders')}</h2>
          <p className="text-3xl font-bold text-blue-600">{activeOrders}</p>
          <p className="text-sm text-gray-600 mt-1">{t('currentlyActiveLentOrders')}</p>
        </div>

        {/* Total Lent Items Card */}
        <div className="bg-white shadow rounded-lg p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-2">{t('totalLentItems')}</h2>
          <p className="text-3xl font-bold text-green-600">{totalLentItems}</p>
          <p className="text-sm text-gray-600 mt-1">{t('totalItemsCurrentlyLentOut')}</p>
        </div>
      </div>

      {/* Active Lent Orders Table */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">{t('activeLentOrders')}</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">{t('lentId')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">{t('employeeId')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">{t('shopName')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">{t('date')}</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {recentOrders.length === 0 ? (
                <tr>
                  <td colSpan="4" className="px-6 py-4 text-center text-gray-500">
                    {t('noActiveOrdersFound')}
                  </td>
                </tr>
              ) : (
                recentOrders.map((order) => (
                  <tr key={order.lentId} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900">{order.lentId}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900">{order.employeeId}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900">{order.shopName}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900">
                      {formatDateTime(order.timestamp)}
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
