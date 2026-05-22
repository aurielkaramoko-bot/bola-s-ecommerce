package com.bolas.ecommerce.config;

import com.bolas.ecommerce.model.Category;
import com.bolas.ecommerce.repository.CategoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;

/**
 * Initialise les 21 grandes familles de catégories BOLA (54 pays d'Afrique).
 * Format DATA : "EMOJI|Nom Famille|SousCat1>feuille1,feuille2|SousCat2>..."
 * Idempotent : ne recrée pas si >= 21 familles racines existent.
 */
@Component
public class CategorySeeder {

    private final CategoryRepository repo;

    public CategorySeeder(CategoryRepository repo) {
        this.repo = repo;
    }

    private static final String[] DATA = {
        "👗|Mode Femme|Hauts & Tops>T-shirts,Chemisiers,Blouses,Débardeurs,Crop tops,Tuniques|Robes>Robes casual,Robes de soirée,Robes longues,Robes boubou,Robes grossesse|Jupes>Jupes courtes,Jupes longues,Jupes portefeuille,Jupes en pagne|Pantalons & Jeans>Jeans,Pantalons larges,Leggings,Combinaisons|Tenues africaines>Boubous femme,Kaba & Slit,Tenues en wax,Tenues en ankara,Robes en kente,Tenues basin|Maillots & Plage>Maillots de bain,Paréos,Tenues de plage|Pyjamas & Lingerie>Pyjamas,Nuisettes,Soutiens-gorge,Culottes,Bodys|Uniformes>Uniformes scolaires fille,Tenues professionnelles,Blouses travail",
        "👔|Mode Homme|Hauts>T-shirts homme,Chemises,Polos,Débardeurs homme,Pulls|Pantalons>Jeans homme,Pantalons chino,Pantalons de jogging,Shorts|Tenues africaines homme>Boubous homme,Agbada,Tenues en wax homme,Kaftan,Dashiki,Tenues basin homme|Costumes & Vestes>Costumes,Vestes de costume,Blazers,Smokings|Sport & Détente>Survêtements,Tenues de sport,Maillots de foot|Sous-vêtements>Boxers,Slips,Chaussettes,Pyjamas homme|Uniformes homme>Uniformes scolaires garçon,Tenues professionnelles homme",
        "🧒|Mode Enfant & Bébé|Bébé 0-2 ans>Bodies,Pyjamas bébé,Grenouillères,Turbulettes,Ensembles bébé|Fille 2-12 ans>Robes fille,Jupes fille,Hauts fille,Leggings fille,Tenues africaines fille|Garçon 2-12 ans>T-shirts garçon,Pantalons garçon,Shorts garçon,Tenues africaines garçon|Ados 12-16 ans>Mode ado fille,Mode ado garçon,Tenues scolaires ado|Uniformes scolaires>Uniformes primaire,Uniformes collège,Uniformes lycée,Blouses|Accessoires enfant>Sacs à dos école,Chapeaux enfant,Ceintures enfant",
        "👟|Chaussures|Chaussures femme>Sandales,Escarpins,Ballerines,Talons hauts,Boots femme,Baskets femme,Tongs|Chaussures homme>Chaussures habillées,Baskets homme,Mocassins,Boots homme,Sandales homme,Chaussures de sport|Chaussures enfant>Chaussures bébé,Sandales enfant,Baskets enfant,Chaussures scolaires|Chaussures artisanales>Babouches,Chaussures en cuir local,Sandales artisanales africaines",
        "👜|Sacs & Maroquinerie|Sacs femme>Sacs à main,Sacs bandoulière,Sacs à dos femme,Pochettes,Sacoches femme|Sacs homme>Sacoches homme,Sacs à dos homme,Porte-documents,Sacs de voyage|Petite maroquinerie>Portefeuilles,Porte-monnaie,Ceintures,Porte-clés|Bagages & Voyage>Valises,Sacs de voyage,Trousses de toilette,Sacs de sport|Sacs artisanaux africains>Sacs en pagne,Sacs en raphia,Paniers en osier,Sacs en cuir local",
        "💍|Bijoux & Accessoires|Bijoux femme>Colliers,Bracelets,Boucles d'oreilles,Bagues,Bracelets de cheville|Bijoux homme>Montres homme,Bracelets homme,Colliers homme,Chevalières|Bijoux artisanaux>Bijoux en perles africaines,Bijoux en bronze,Bijoux en bois,Bijoux wax|Accessoires mode>Ceintures,Écharpes & Foulards,Chapeaux & Casquettes,Lunettes de soleil,Montres femme|Accessoires cheveux>Barrettes,Chouchous,Turbans,Headbands,Perles cheveux",
        "🪡|Pagnes & Tissus|Pagnes imprimés>Wax hollandais,Wax africain,Super wax,Ankara,Kente,Bogolan|Tissus brodés>Basin riche,Basin simple,Getzner,Tissu brocard|Autres tissus>Coton uni,Soie,Velours,Dentelle,Mousseline|Couture & Mercerie>Fil à coudre,Boutons,Fermetures éclair,Élastiques,Patrons de couture|Uniformes sur mesure>Commande uniforme entreprise,Commande tenue scolaire",
        "💄|Beauté & Soins|Soins visage>Crèmes hydratantes,Sérums,Gommages visage,Masques,Contour des yeux,Crèmes anti-taches|Soins corps>Lotions corps,Beurre de karité,Huile de coco,Gels douche,Savons naturels,Déodorants|Soins cheveux>Shampooings,Après-shampooings,Masques cheveux,Huiles capillaires,Laits capillaires,Activateurs de boucles|Maquillage>Fonds de teint,Correcteurs,Rouges à lèvres,Mascaras,Fards à paupières,Blush,Highlighters|Parfums>Parfums femme,Parfums homme,Huiles parfumées,Encens & Bakhour|Hygiène>Savons hygiène,Dentifrices,Brosses à dents,Produits intimes,Cotons & Lingettes|Produits naturels>Karité pur,Huile d'argan,Aloe vera,Neem,Rhassoul,Moringa|Ongles>Vernis à ongles,Gel UV,Faux ongles,Accessoires manucure",
        "💇|Cheveux & Perruques|Perruques>Perruques lace front,Perruques full lace,Perruques synthétiques,Perruques cheveux naturels|Extensions>Tresses synthétiques,Cheveux tissage,Clip-in extensions,Crochet braids|Tresses & Nattes>Tresses kanekalon,Expression braids,Marley hair,Twist hair|Accessoires coiffure>Sèche-cheveux,Fers à lisser,Fers à friser,Bonnets de nuit,Peignes & Brosses",
        "📱|Téléphones & Électronique|Téléphones>Smartphones neufs,Smartphones reconditionnés,Téléphones basiques|Accessoires mobile>Coques & Housses,Protections écran,Chargeurs,Câbles,Écouteurs,Powerbanks,Supports téléphone|Ordinateurs>Laptops neufs,Laptops reconditionnés,Tablettes,Accessoires PC|Audio & Vidéo>Écouteurs & Casques,Enceintes Bluetooth,TV,Décodeurs TNT,Câbles HDMI|Électroménager>Ventilateurs,Climatiseurs,Réfrigérateurs,Machines à laver,Mixeurs,Fers à repasser,Cuisinières|Énergie solaire>Panneaux solaires,Batteries solaires,Lampes solaires,Chargeurs solaires,Groupes électrogènes|Jeux vidéo>Consoles,Manettes,Jeux,Accessoires gaming",
        "🏠|Maison & Décoration|Meubles>Canapés,Lits,Armoires,Tables,Chaises,Étagères,Bureaux|Décoration>Tableaux & Cadres,Vases,Bougies,Coussins déco,Tapis,Rideaux,Miroirs|Literie>Draps,Couettes,Oreillers,Couvertures,Protège-matelas|Cuisine>Casseroles & Marmites,Poêles,Ustensiles,Couteaux,Mortiers,Assiettes & Couverts|Rangement>Paniers de rangement,Boîtes de rangement,Armoires plastique|Décoration africaine>Masques décoratifs,Statuettes,Tissus muraux,Calebasses décoratives,Objets en bois sculpté|Jardinage>Plantes en pot,Graines & Semences,Terreau,Outils de jardinage,Pots de fleurs",
        "🏗️|Construction & BTP|Matériaux gros oeuvre>Ciment,Fer à béton,Parpaings,Sable & Gravier,Tuiles & Toitures|Peinture & Revêtement>Peintures intérieures,Peintures extérieures,Vernis,Enduits,Carrelages|Plomberie>Tuyaux,Robinets,Lavabos,WC,Pompes à eau,Châteaux d'eau|Électricité>Câbles électriques,Interrupteurs,Prises,Disjoncteurs,Ampoules LED|Outillage>Marteaux,Tournevis,Perceuses,Meuleuses,Niveaux,Scies|Quincaillerie>Visserie & Boulonnerie,Serrures & Cadenas,Charnières,Grillages",
        "🥘|Alimentation & Boissons|Céréales & Féculents>Riz,Maïs,Mil & Sorgho,Fonio,Igname,Manioc,Plantain|Légumes & Fruits>Légumes frais locaux,Fruits tropicaux,Herbes aromatiques,Piments|Viandes & Poissons>Viande boeuf,Viande mouton,Poulet,Poissons frais,Poissons fumés,Crevettes|Condiments & Épices>Huile de palme,Huile d'arachide,Soumbala,Poudre de gombo,Épices locales,Cube Maggi|Produits transformés>Farines locales,Attiéké,Gari,Arachides & Pâte d'arachide,Huile de coco alimentaire|Boissons>Jus de fruits locaux,Bissap,Gingembre,Sobolo,Eau minérale,Lait en poudre|Pâtisserie & Snacks>Gâteaux locaux,Beignets,Chinchins,Cacahuètes grillées,Noix de cajou|Restauration>Plats préparés,Traiteur,Livraison repas",
        "💊|Santé & Pharmacie|Médicaments courants>Antidouleurs,Antipaludéens,Vitamines & Compléments,Antidiarrhéiques|Matériel médical>Tensiomètres,Thermomètres,Glucomètres,Oxymètres,Pansements|Médecine traditionnelle>Plantes médicinales,Décoctions,Poudres traditionnelles,Tisanes africaines|Hygiène bébé>Couches,Lingettes bébé,Crèmes bébé,Laits bébé,Biberons|Optique>Lunettes de vue,Lunettes de soleil médicales,Lentilles",
        "🌾|Agriculture & Élevage|Semences & Plants>Semences légumes,Semences céréales,Plants fruitiers,Plants maraîchers|Intrants agricoles>Engrais organiques,Engrais chimiques,Pesticides,Herbicides,Fongicides|Matériel agricole>Houes & Daba,Machettes,Brouettes,Semoirs,Motoculteurs,Pompes d'irrigation|Élevage>Aliments volaille,Aliments bétail,Poussins & Oeufs à couver,Médicaments vétérinaires,Équipements d'élevage|Pêche>Filets de pêche,Lignes & Hameçons,Pirogues & Accessoires,Glacières de pêche|Irrigation>Tuyaux d'irrigation,Goutteurs,Pompes solaires,Réservoirs",
        "🚗|Automobile & Moto|Pièces auto>Filtres,Plaquettes de frein,Amortisseurs,Courroies,Bougies,Batteries auto|Pièces moto>Pneus moto,Chaînes moto,Pièces moteur moto,Casques|Accessoires auto>Tapis de voiture,Housses de siège,Chargeurs voiture,GPS,Caméras de recul|Entretien auto>Huiles moteur,Graisses,Liquide de refroidissement,Produits de nettoyage auto|Vélos & Trottinettes>Vélos,Vélos électriques,Trottinettes,Pièces vélo",
        "📚|Éducation & Livres|Livres>Manuels scolaires,Livres parascolaires,Romans africains,Bandes dessinées,Livres religieux|Fournitures scolaires>Cahiers,Stylos & Crayons,Règles & Équerre,Sacs à dos scolaires,Calculatrices,Compas|Matériel de bureau>Ramettes de papier,Imprimantes,Cartouches d'encre,Agrafeuses,Classeurs|Arts & Loisirs créatifs>Peintures artistiques,Toiles,Pinceaux,Argile & Poterie,Matériel de broderie|Musique>Guitares,Djembés,Balafons,Koras,Claviers,Microphones",
        "⚽|Sport & Loisirs|Football>Ballons de foot,Maillots de foot,Crampons,Protège-tibias,Filets de but|Fitness & Gym>Haltères,Tapis de sport,Cordes à sauter,Élastiques musculation,Bancs de musculation|Sports d'eau>Maillots de bain homme,Lunettes de natation,Bouées,Palmes|Sports de raquette>Raquettes de tennis,Balles de tennis,Raquettes de badminton|Jeux & Jouets>Jouets bébé,Jeux de société,Poupées,Voitures jouet,Jeux éducatifs|Camping & Plein air>Tentes,Sacs de couchage,Lampes torches,Machettes camping",
        "🐾|Animaux & Accessoires|Chiens>Croquettes chien,Friandises chien,Laisses & Colliers,Jouets chien,Cages & Niches|Chats>Croquettes chat,Litière,Griffoirs,Jouets chat,Transportins|Oiseaux & Volailles>Aliments oiseaux,Cages oiseaux,Accessoires volaille|Vétérinaire>Antiparasitaires,Vaccins,Vitamines animaux,Pansements vétérinaires",
        "🎭|Artisanat Africain|Sculptures & Bois>Masques en bois,Statuettes,Instruments en bois,Mortiers sculptés,Tabourets Ashanti|Poterie & Céramique>Canaris,Jarres décoratives,Bols en terre cuite,Théières africaines|Vannerie & Raphia>Paniers en raphia,Nattes tressées,Chapeaux en paille,Dessous-de-plat|Peinture & Art>Peintures sur tissu,Batik,Peintures sur toile,Affiches artistiques|Bijoux artisanaux africains>Bijoux en perles,Bijoux en bronze,Bijoux en corne,Bijoux en cuir|Instruments traditionnels>Djembé,Kora,Balafon,Tam-tam,Sanza",
        "🛠️|Services & Prestations|Coiffure & Beauté>Coiffure à domicile,Maquillage événement,Soins spa à domicile,Tressage|Couture & Retouche>Couture sur mesure,Retouches vêtements,Création tenues africaines|Événementiel>Décoration mariage,Traiteur événement,Location tentes & chaises,Sono & DJ|Réparation>Réparation téléphones,Réparation électroménager,Réparation auto,Cordonnerie|Services numériques>Création site web,Graphisme,Impression & Communication,Photos & Vidéos|Immobilier>Location appartement,Location maison,Vente terrain,Location boutique",
        "💪|Nutrition Sportive|Protéines>Whey protéine,Protéine végétale,Caséine,Isolat de protéine|Prise de masse>Gainers,Créatine,Dextrose,Maltodextrine|Acides aminés>BCAA,Glutamine,Arginine,Bêta-alanine|Perte de poids>Brûleurs de graisse,L-Carnitine,CLA,Thé vert extrait|Énergie & Endurance>Boissons isotoniques,Gels énergétiques,Barres protéinées,Caféine sport|Vitamines & Santé>Multivitamines,Oméga 3,Magnésium,Zinc,Vitamine C & D|Accessoires nutrition>Shakers,Boîtes compartiments,Balances alimentaires,Bouteilles sport",
        "💒|Mariage & Événements|Tenues mariage>Robes de mariée,Costumes mariés,Tenues demoiselles d'honneur,Tenues invités mariage|Décoration mariage>Centres de table,Ballons & Guirlandes,Arches florales,Nappes & Housses chaises|Faire-part & Papeterie>Faire-part mariage,Faire-part baptême,Menus,Plans de table,Livres d'or|Cadeaux & Souvenirs>Cadeaux invités,Boîtes à dragées,Souvenirs personnalisés,Albums photo|Baptême & Anniversaire>Décorations baptême,Gâteaux événement,Tenues baptême bébé,Cadeaux naissance",
        "🕌|Mode Modeste & Religion|Mode musulmane femme>Hijabs & Voiles,Abayas,Jilbabs,Turbans femme,Robes longues modestes|Mode musulmane homme>Djellabas,Gandouras,Kufis & Calottes,Chapeaux de prière|Accessoires prière>Tapis de prière,Chapelets,Corans & Livres islamiques,Encens & Bakhour|Mode chrétienne>Tenues église femme,Tenues église homme,Robes de choeur,Croix & Bijoux religieux|Spiritualité africaine>Objets rituels,Encens africains,Huiles spirituelles,Livres spirituels",
        "✂️|Coiffure Pro & Salon|Matériel coiffure>Ciseaux professionnels,Tondeuses,Rasoirs,Séchoirs pro,Fers à lisser pro|Produits coiffure pro>Colorations cheveux,Décolorants,Permanentes,Lissages brésiliens,Kératine|Mobilier salon>Fauteuils de coiffure,Bacs à shampooing,Miroirs salon,Chariots coiffure|Consommables salon>Tabliers coiffure,Bonnets plastique,Gants latex,Pinces & Barrettes pro|Ongles & Nail art>Lampes UV/LED,Gels UV pro,Acrylique ongles,Ponceuses ongles,Nail art",
        "🍼|Bébé & Puériculture|Alimentation bébé>Lait infantile 1er âge,Lait 2e âge,Petits pots,Céréales bébé,Biberons & Tétines|Hygiène bébé>Couches jetables,Couches lavables,Lingettes bébé,Crème change,Savon bébé|Couchage & Sécurité>Lits bébé,Matelas bébé,Moustiquaires bébé,Baby monitors,Barrières sécurité|Éveil & Développement>Tapis d'éveil,Mobiles musicaux,Anneaux dentition,Hochets,Portiques activité|Transport bébé>Poussettes,Porte-bébés & Écharpes,Sièges auto bébé,Couffins,Sacs à langer|Bain bébé>Baignoires bébé,Thermomètres bain,Sièges de bain,Serviettes à capuche",
        "🖨️|Impression & Communication|Impression>Flyers & Prospectus,Affiches,Bannières roll-up,Cartes de visite,Brochures|Signalétique>Enseignes lumineuses,Panneaux publicitaires,Stickers & Autocollants,Bâches imprimées|Cadeaux entreprise>Stylos personnalisés,T-shirts publicitaires,Mugs personnalisés,Casquettes pub|Papeterie entreprise>En-têtes de lettre,Tampons encreurs,Enveloppes personnalisées,Blocs-notes",
        "⚡|Énergie & Électricité|Énergie solaire>Panneaux solaires,Régulateurs solaires,Batteries solaires,Kits solaires complets,Onduleurs solaires|Groupes électrogènes>Groupes essence,Groupes diesel,Groupes inverter silencieux|Éclairage>Ampoules LED,Lampes solaires,Lampadaires solaires,Guirlandes lumineuses,Torches & Frontales|Eau & Pompage>Pompes à eau solaires,Pompes immergées,Châteaux d'eau,Filtres à eau,Purificateurs|Stabilisateurs>Stabilisateurs de tension,Onduleurs UPS,Parasurtenseurs,Multiprises protégées",
        "🎒|Scolaire & Formation|Fournitures primaire>Ardoises & Craies,Crayons de couleur,Cahiers grands carreaux,Règles & Équerre|Fournitures lycée>Calculatrices scientifiques,Compas & Rapporteurs,Cahiers petits carreaux,Stylos plumes|Sacs scolaires>Cartables primaire,Sacs à dos lycée,Trousses,Sacs de sport école|Parascolaire>Livres de révision,Cahiers de vacances,Dictionnaires,Encyclopédies jeunesse|Formation pro>Livres de formation métier,Kits couture formation,Kits coiffure formation,Matériels cours"
    };

    @Transactional
    public void seed() {
        // Idempotent : si déjà 30 familles racines → ne rien faire
        if (repo.countByParentIdIsNull() >= 30) return;

        // Récupérer les slugs existants pour éviter les doublons
        java.util.Set<String> existingSlugs = repo.findAll().stream()
                .map(Category::getSlug)
                .filter(s -> s != null)
                .collect(java.util.stream.Collectors.toSet());

        for (String line : DATA) {
            String[] parts = line.split("\\|", 3);
            if (parts.length < 2) continue;
            String emoji    = parts[0];
            String rootName = parts[1];
            String rootSlug = slugify(rootName);

            // Ne pas recréer si déjà existant
            if (existingSlugs.contains(rootSlug)) continue;

            Category root = saveCat(null, rootName, rootSlug, emoji);
            existingSlugs.add(rootSlug);

            if (parts.length < 3 || parts[2].isBlank()) continue;

            for (String subPart : parts[2].split("\\|")) {
                String[] sv = subPart.split(">", 2);
                String subName = sv[0].trim();
                String subSlug = rootSlug + "-" + slugify(subName);

                Category sub;
                if (existingSlugs.contains(subSlug)) {
                    sub = repo.findBySlug(subSlug).orElse(saveCat(root.getId(), subName, subSlug, null));
                } else {
                    sub = saveCat(root.getId(), subName, subSlug, null);
                    existingSlugs.add(subSlug);
                }

                if (sv.length < 2 || sv[1].isBlank()) continue;

                for (String leaf : sv[1].split(",")) {
                    String leafName = leaf.trim();
                    String leafSlug = subSlug + "-" + slugify(leafName);
                    if (!existingSlugs.contains(leafSlug)) {
                        saveCat(sub.getId(), leafName, leafSlug, null);
                        existingSlugs.add(leafSlug);
                    }
                }
            }
        }
    }

    private Category saveCat(Long parentId, String name, String slug, String emoji) {
        Category c = new Category();
        c.setParentId(parentId);
        c.setName(name);
        c.setSlug(slug);
        c.setEmoji(emoji);
        c.setActive(true);
        return repo.save(c);
    }

    /** Génère un slug URL-friendly depuis un nom (sans accents, minuscules, tirets) */
    public static String slugify(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
