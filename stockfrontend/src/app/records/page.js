'use client';
import { useState, useEffect } from 'react';
import { recordsApi } from '@/services/api';
import { useTranslation } from 'react-i18next';

export default function Records() {
  const { i18n, t } = useTranslation();
  const [activeTab, setActiveTab] = useState('all');
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  // New state for search, sort, and pagination
  const [searchTerm, setSearchTerm] = useState('');
  const [sortField, setSortField] = useState('timestamp');
  const [sortDirection, setSortDirection] = useState('desc');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [pageSize] = useState(20);

  // Fetch records based on active tab and filters
  const fetchRecords = async () => {
    setLoading(true);
    setError(null);
    try {
      let data;
      let totalPagesCount = 0;
      switch (activeTab) {
        case 'sales':
          const salesResponse = await recordsApi.getSalesRecords(currentPage, pageSize, sortField, sortDirection);
          data = salesResponse.content;
          totalPagesCount = salesResponse.totalPages;
          break;
        case 'lent':
          const lentResponse = await recordsApi.getLentRecords(currentPage, pageSize, sortField, sortDirection);
          data = lentResponse.content;
          totalPagesCount = lentResponse.totalPages;
          break;
        case 'broken':
          // Temporarily disabled broken records
          data = [];
          totalPagesCount = 0;
          break;
        default:
          // For 'all', fetch from sales and lent only
          const [salesData, lentData] = await Promise.all([
            recordsApi.getSalesRecords(currentPage, pageSize, sortField, sortDirection),
            recordsApi.getLentRecords(currentPage, pageSize, sortField, sortDirection)
          ]);
          data = [
            ...(salesData.content || []),
            ...(lentData.content || [])
          ].sort((a, b) => {
            const aValue = a[sortField];
            const bValue = b[sortField];
            if (sortDirection === 'asc') {
              return aValue > bValue ? 1 : -1;
            }
            return aValue < bValue ? 1 : -1;
          });
          // Calculate total pages for combined data
          totalPagesCount = Math.max(
            salesData.totalPages,
            lentData.totalPages
          );
      }

      // Apply search filter
      if (searchTerm) {
        data = data.filter(record => 
          record.orderId?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          record.employeeId?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          record.shopName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          record.productBarcode?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          record.productName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          (record.status || record.condition)?.toLowerCase().includes(searchTerm.toLowerCase())
        );
      }

      setRecords(data || []);
      setTotalPages(totalPagesCount);
    } catch (err) {
      console.error('Error fetching records:', err);
      setError(err.message || 'Failed to fetch records');
      setRecords([]);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  };

  // Fetch records when tab, page, sort, or search changes
  useEffect(() => {
    fetchRecords();
  }, [activeTab, currentPage, sortField, sortDirection, searchTerm]);

  // Function to get status display
  const getStatusDisplay = (record) => {
    if (record.type === 'lent') {
      return t(record.status) || '-';
    }
    if (record.type === 'broken') {
      return t(record.condition) || '-';
    }
    return t('sold');
  };

  // Function to handle sort
  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
    setCurrentPage(0); // Reset to first page when sorting changes
  };

  // Function to render sort indicator
  const renderSortIndicator = (field) => {
    if (sortField !== field) return '↕';
    return sortDirection === 'asc' ? '↑' : '↓';
  };

  const toggleLanguage = () => {
    const newLang = i18n.language === 'en' ? 'th' : 'en';
    i18n.changeLanguage(newLang);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-black">{t('records')}</h1>
      </div>

      {/* Record Type Tabs */}
      <div className="bg-white shadow rounded-lg">
        <div className="border-b border-gray-200">
          <nav className="-mb-px flex space-x-8 px-4" aria-label="Tabs">
            {['all', 'sales', 'lent'].map((tab) => (
              <button
                key={tab}
                onClick={() => {
                  setActiveTab(tab);
                  setCurrentPage(0); // Reset to first page when changing tabs
                }}
                className={`${
                  activeTab === tab
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm capitalize`}
              >
                {tab === 'all' ? t('allRecords') : t(tab)}
              </button>
            ))}
          </nav>
        </div>

        {/* Search and Filter */}
        <div className="p-4">
          <div className="flex gap-4 items-center">
            <div className="flex-1">
              <input
                type="text"
                placeholder={t('searchByOrderIdEmployeeIdShopNameStatus')}
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setCurrentPage(0); // Reset to first page when search changes
                }}
                className="flex-1 rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900 placeholder-gray-400 w-full"
              />
            </div>
          </div>
        </div>

        {/* Records Table */}
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th 
                  scope="col" 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('orderId')}
                >
                  {t('orderId')} {renderSortIndicator('orderId')}
                </th>
                {activeTab === 'all' && (
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                    {t('type')}
                  </th>
                )}
                <th 
                  scope="col" 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('employeeId')}
                >
                  {t('employeeId')} {renderSortIndicator('employeeId')}
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  {t('productName')}
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  {t('productBarcode')}
                </th>
                <th 
                  scope="col" 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('shopName')}
                >
                  {t('shopName')} {renderSortIndicator('shopName')}
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  {t('quantity')}
                </th>
                <th 
                  scope="col" 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('status')}
                >
                  {t('statusCondition')} {renderSortIndicator('status')}
                </th>
                <th 
                  scope="col" 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('timestamp')}
                >
                  {t('date')} {renderSortIndicator('timestamp')}
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {loading ? (
                <tr>
                  <td className="px-6 py-4 text-gray-700 text-center" colSpan="7">
                    {t('loadingRecords')}
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td className="px-6 py-4 text-red-600 text-center" colSpan="7">
                    {error}
                  </td>
                </tr>
              ) : records.length === 0 ? (
                <tr>
                  <td className="px-6 py-4 text-gray-700 text-center" colSpan="7">
                    {t('noRecordsFound')}
                  </td>
                </tr>
              ) : (
                records.map((record, index) => (
                  <tr key={`${record.orderId}-${index}`} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {record.orderId}
                    </td>
                    {activeTab === 'all' && (
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700 capitalize">
                        {t(record.type)}
                      </td>
                    )}
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {record.employeeId}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {record.productName}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {record.productBarcode}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {record.shopName || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {record.quantity ?? 1}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {getStatusDisplay(record)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {new Date(record.timestamp).toLocaleString('th-TH', {
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

        {/* Pagination */}
        <div className="px-4 py-3 flex items-center justify-between border-t border-gray-200">
          <div className="flex-1 flex justify-between items-center">
            <div>
              <p className="text-sm text-gray-700">
                {t('page')} {currentPage + 1} {t('of')} {totalPages}
              </p>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                disabled={currentPage === 0}
                className={`relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md ${
                  currentPage === 0
                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                    : 'bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                {t('previous')}
              </button>
              <button
                onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                disabled={currentPage >= totalPages - 1}
                className={`relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md ${
                  currentPage >= totalPages - 1
                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                    : 'bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                {t('next')}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
} 