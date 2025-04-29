import { Inter } from 'next/font/google'
import './globals.css'
import ClientNav from '@/components/ClientNav'

const inter = Inter({ subsets: ['latin'] })

export const metadata = {
  title: 'Inventory Management System',
  description: 'Stock management and inventory tracking system',
}

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <ClientNav>
          {children}
        </ClientNav>
        {/* Main Content */}
        <div className="min-h-screen bg-gray-100">
          <div className="pl-64">
            <main className="p-8">
              {children}
            </main>
          </div>
        </div>
      </body>
    </html>
  )
}
