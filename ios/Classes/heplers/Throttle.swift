//
//  Throttle.swift
//  vietmap_flutter_navigation
//
//  Created by dev on 22/8/24.
//

import Foundation 

class Throttler {
    private var workItem: DispatchWorkItem?
    private var lastExecution: Date?
    private let queue: DispatchQueue
    private let minimumDelay: TimeInterval

    init(minimumDelay: TimeInterval, queue: DispatchQueue = DispatchQueue.main) {
        self.minimumDelay = minimumDelay
        self.queue = queue
    }

    func throttle(_ block: @escaping () -> Void) {
        if let lastExecution = lastExecution {
            let interval = Date().timeIntervalSince(lastExecution)
            if interval < minimumDelay {
                return
            }
        }

        workItem?.cancel()
        
        let work = DispatchWorkItem {
            block()
            self.lastExecution = Date()
        }
        workItem = work
        queue.async(execute: work)
    }
}
