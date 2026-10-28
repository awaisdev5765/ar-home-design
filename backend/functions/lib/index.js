"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.buildSharePackage = exports.createShareLink = exports.generateEstimate = exports.createProject = void 0;
const https_1 = require("firebase-functions/https");
const admin = __importStar(require("firebase-admin"));
admin.initializeApp();
const db = admin.firestore();
exports.createProject = (0, https_1.onCall)(async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "Authentication required.");
    }
    const data = request.data;
    const title = (data.title || "").trim();
    if (!title) {
        throw new https_1.HttpsError("invalid-argument", "Project title is required.");
    }
    const now = admin.firestore.FieldValue.serverTimestamp();
    const ownerId = request.auth.uid;
    const projectRef = db.collection("projects").doc();
    await projectRef.set({
        ownerId,
        title,
        description: data.description ?? "",
        propertyType: data.propertyType ?? "apartment",
        status: "draft",
        createdAt: now,
        updatedAt: now
    });
    return {
        projectId: projectRef.id,
        message: "Project created successfully."
    };
});
exports.generateEstimate = (0, https_1.onCall)(async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "Authentication required.");
    }
    const data = request.data;
    if (!data.projectId) {
        throw new https_1.HttpsError("invalid-argument", "projectId is required.");
    }
    const ownerId = request.auth.uid;
    const region = data.region ?? "UAE";
    const overlays = data.overlays ?? [];
    const fallbackCount = Math.max(1, data.overlayCount ?? 1);
    const catalogSnapshot = await db.collection("materialCatalog").get();
    const catalogByPreset = new Map();
    catalogSnapshot.docs.forEach((doc) => {
        const row = doc.data();
        const key = String(row.presetCode ?? doc.id);
        catalogByPreset.set(key, {
            priceUae: Number(row.priceUae ?? 120),
            priceUs: Number(row.priceUs ?? 40)
        });
    });
    const lineItems = (overlays.length > 0 ? overlays : new Array(fallbackCount).fill({ materialPreset: "default_paint" }))
        .map((overlay, index) => {
        const preset = overlay.materialPreset || "default_paint";
        const price = catalogByPreset.get(preset) ?? { priceUae: 120, priceUs: 40 };
        const unitRate = region === "UAE" ? price.priceUae : price.priceUs;
        const quantity = Math.max(1, Number(overlay.widthMeters ?? 1) * Number(overlay.heightMeters ?? 1));
        return {
            label: `Material estimate #${index + 1} (${preset})`,
            quantity,
            unitRate,
            lineTotal: quantity * unitRate
        };
    });
    const subtotal = lineItems.reduce((sum, row) => sum + row.lineTotal, 0);
    const taxRate = region === "UAE" ? 0.05 : 0.08;
    const tax = subtotal * taxRate;
    const total = subtotal + tax;
    const currency = region === "UAE" ? "AED" : "USD";
    const estimateRef = db.collection("estimates").doc();
    await estimateRef.set({
        projectId: data.projectId,
        roomId: data.roomId ?? "",
        versionId: "",
        ownerId,
        region,
        lineItems,
        subtotal,
        tax,
        total,
        currency,
        generatedBy: "system",
        createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    return {
        estimateId: estimateRef.id,
        total,
        currency,
        message: "Estimate generated successfully."
    };
});
exports.createShareLink = (0, https_1.onCall)(async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "Authentication required.");
    }
    const data = request.data;
    if (!data.projectId) {
        throw new https_1.HttpsError("invalid-argument", "projectId is required.");
    }
    const ownerId = request.auth.uid;
    const projectSnap = await db.collection("projects").doc(data.projectId).get();
    if (!projectSnap.exists || projectSnap.get("ownerId") !== ownerId) {
        throw new https_1.HttpsError("permission-denied", "Project ownership validation failed.");
    }
    const rateBucketRef = db.collection("shareRateLimits").doc(`${ownerId}_${data.projectId}`);
    const rateBucketSnap = await rateBucketRef.get();
    const minuteWindowStart = Date.now() - 60 * 1000;
    const recentTimestamps = (rateBucketSnap.get("timestamps") ?? [])
        .filter((ts) => typeof ts === "number" && ts >= minuteWindowStart);
    if (recentTimestamps.length >= 5) {
        throw new https_1.HttpsError("resource-exhausted", "Too many share-link requests. Please wait a minute.");
    }
    const expiresInDays = Math.max(1, Math.min(30, data.expiresInDays ?? 7));
    const now = Date.now();
    const expiresAt = new Date(now + expiresInDays * 24 * 60 * 60 * 1000);
    const tokenRaw = `${ownerId}:${data.projectId}:${now}`;
    const tokenHash = Buffer.from(tokenRaw).toString("base64url");
    const ref = db.collection("shareLinks").doc();
    await ref.set({
        projectId: data.projectId,
        versionId: data.versionId ?? "",
        ownerId,
        tokenHash,
        expiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
        createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    await rateBucketRef.set({
        ownerId,
        projectId: data.projectId,
        timestamps: [...recentTimestamps, now],
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
    const shareUrl = `https://arhomedesign.app/share/${tokenHash}`;
    return {
        shareId: ref.id,
        shareUrl,
        expiresAt: expiresAt.toISOString()
    };
});
exports.buildSharePackage = (0, https_1.onCall)(async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "Authentication required.");
    }
    const data = request.data;
    if (!data.projectId) {
        throw new https_1.HttpsError("invalid-argument", "projectId is required.");
    }
    const ownerId = request.auth.uid;
    const projectSnap = await db.collection("projects").doc(data.projectId).get();
    if (!projectSnap.exists || projectSnap.get("ownerId") !== ownerId) {
        throw new https_1.HttpsError("permission-denied", "Project ownership validation failed.");
    }
    let latestVersionQuery = db.collection("designVersions")
        .where("ownerId", "==", ownerId)
        .where("projectId", "==", data.projectId)
        .orderBy("versionNumber", "desc")
        .limit(1);
    if (data.roomId) {
        latestVersionQuery = latestVersionQuery.where("roomId", "==", data.roomId);
    }
    let latestEstimateQuery = db.collection("estimates")
        .where("ownerId", "==", ownerId)
        .where("projectId", "==", data.projectId)
        .orderBy("createdAt", "desc")
        .limit(1);
    if (data.roomId) {
        latestEstimateQuery = latestEstimateQuery.where("roomId", "==", data.roomId);
    }
    const [versionSnap, estimateSnap, overlaySnap] = await Promise.all([
        latestVersionQuery.get(),
        latestEstimateQuery.get(),
        db.collection("overlayRegions")
            .where("ownerId", "==", ownerId)
            .where("projectId", "==", data.projectId)
            .get()
    ]);
    const latestVersion = versionSnap.docs[0]?.data() ?? null;
    const latestEstimate = estimateSnap.docs[0]?.data() ?? null;
    const overlays = overlaySnap.docs.map((doc) => doc.data());
    const payload = {
        projectId: data.projectId,
        roomId: data.roomId ?? "",
        generatedAt: new Date().toISOString(),
        latestVersion,
        latestEstimate,
        overlayCount: overlays.length,
        overlays
    };
    return payload;
});
