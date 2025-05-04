import { Inter } from 'next/font/google'
import './globals.css'
import LayoutWithNav from './LayoutWithNav'

const inter = Inter({ subsets: ['latin'] })

export const metadata = {
  title: 'Inventory Management System',
  description: 'Stock management and inventory tracking system',
}

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <LayoutWithNav>{children}</LayoutWithNav>
      </body>
    </html>
  )
}
