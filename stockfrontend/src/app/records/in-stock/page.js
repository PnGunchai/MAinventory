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
    const [page, setPage] = useState(0);
    const [pageSize, setPageSize] = useState(20);
    const [totalPages, setTotalPages] = useState(1);
    const [totalElements, setTotalElements] = useState(0);

    useEffect(() => {
        fetchInStockItems({ page: 0 });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [searchTerm, pageSize]);

    useEffect(() => {
        fetchInStockItems();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page]);

    const fetchInStockItems = async (opts = {}) => {
        try {
            setLoading(true);
            const response = await inStockApi.getPaginatedInStock({
                page: opts.page ?? page,
                size: opts.pageSize ?? pageSize,
                search: opts.searchTerm ?? searchTerm
            });
            setInStockItems(response.content || []);
            setTotalPages(response.totalPages || 1);
            setTotalElements(response.totalElements || 0);
            setPage(response.page || 0);
            setError(null);
        } catch (err) {
            setError('Failed to fetch in-stock items. Please try again later.');
            console.error('Error fetching in-stock items:', err);
        } finally {
            setLoading(false);
        }
    };

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
                                onChange={(e) => { setSearchTerm(e.target.value); setPage(0); }}
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
                                {inStockItems.map((item) => (
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
                                {inStockItems.length === 0 && (
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
                {/* Pagination Controls */}
                <div className="flex justify-between items-center p-4 border-t border-gray-200">
                    <div className="text-sm text-gray-900">
                        {t('showingResults', { productsLength: inStockItems.length, totalItems: totalElements })}
                    </div>
                    <div className="flex items-center space-x-4">
                        <select
                            className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                            value={pageSize}
                            onChange={(e) => { setPageSize(Number(e.target.value)); setPage(0); }}
                        >
                            <option value="20">{t('perPage20') || '20 / page'}</option>
                            <option value="50">{t('perPage50') || '50 / page'}</option>
                            <option value="100">{t('perPage100') || '100 / page'}</option>
                        </select>
                        <div className="space-x-2">
                            <button
                                className="px-3 py-1 border rounded text-gray-900 hover:bg-gray-50"
                                onClick={() => setPage(Math.max(0, page - 1))}
                                disabled={page === 0 || loading}
                            >
                                {t('previous')}
                            </button>
                            <button
                                className="px-3 py-1 border rounded text-gray-900 hover:bg-gray-50"
                                onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                                disabled={page >= totalPages - 1 || loading}
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