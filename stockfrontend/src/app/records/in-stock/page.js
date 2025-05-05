'use client';

import { useState, useEffect } from 'react';
import { inStockApi } from '@/services/api';
import { formatDateTime } from '@/utils/dateUtils';
import { useTranslation } from 'react-i18next';

export default function InStockPage() {
    const { t } = useTranslation();
    const [inStockItems, setInStockItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        fetchInStockItems();
    }, []);

    const fetchInStockItems = async () => {
        try {
            setLoading(true);
            const data = await inStockApi.getAllInStock();
            setInStockItems(data);
            setError(null);
        } catch (err) {
            setError('Failed to fetch in-stock items. Please try again later.');
            console.error('Error fetching in-stock items:', err);
        } finally {
            setLoading(false);
        }
    };

    const filteredItems = inStockItems.filter(item => {
        const searchLower = searchTerm.toLowerCase();
        return (
            item.productName?.toLowerCase().includes(searchLower) ||
            item.boxBarcode?.toLowerCase().includes(searchLower) ||
            (item.productBarcode && item.productBarcode.toLowerCase().includes(searchLower)) ||
            (item.boxNumber && item.boxNumber.toString().includes(searchTerm))
        );
    });

    if (error) {
        return (
            <div className="p-4 text-center">
                <p className="text-red-500">{t('failedToFetchInStockItems')}</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <h1 className="text-3xl font-bold text-black">{t('inStockBarcode')}</h1>

            {/* Search */}
            <div className="bg-white shadow rounded-lg">
                <div className="p-4">
                    <div className="flex gap-4 items-center">
                        <div className="flex-1">
                            <input
                                type="text"
                                placeholder={t('searchByProductNameBoxNumberBarcode')}
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="flex-1 rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900 placeholder-gray-400 w-full"
                            />
                        </div>
                    </div>
                </div>

                {/* Table */}
                <div className="overflow-x-auto">
                    {loading ? (
                        <div className="flex justify-center items-center p-8">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
                        </div>
                    ) : (
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                                        {t('productName')}
                                    </th>
                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                                        {t('boxBarcode')}
                                    </th>
                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                                        {t('productBarcode')}
                                    </th>
                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                                        {t('boxNumber')}
                                    </th>
                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                                        {t('addedDate')}
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {filteredItems.map((item) => (
                                    <tr key={item.id} className="hover:bg-gray-50">
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{item.productName}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{item.boxBarcode}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{item.productBarcode || '-'}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{item.boxNumber || '-'}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                            {item.addedTimestamp ? formatDateTime(item.addedTimestamp) : '-'}
                                        </td>
                                    </tr>
                                ))}
                                {filteredItems.length === 0 && (
                                    <tr>
                                        <td colSpan={5} className="px-6 py-4 text-center text-sm text-gray-500">
                                            {t('noItemsFound')}
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
} 